/////////////////////////////////////////////////////////////////////////
// Project Shared Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common.AUTOIIDFTEST;

import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;


import com.k2view.cdbms.usercode.common.IIDFProducerSingleton;
import com.k2view.fabric.parser.statement.select.PlainSelect;
import com.k2view.fabric.parser.statement.select.Select;
import com.k2view.fabric.parser.analyzer.SqlStatementAnalyzer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.k2view.fabric.parser.JSQLParserException;
import com.k2view.fabric.parser.expression.Expression;
import com.k2view.fabric.parser.expression.operators.relational.ExpressionList;
import com.k2view.fabric.parser.parser.CCJSqlParserManager;
import com.k2view.fabric.parser.schema.Column;
import com.k2view.fabric.parser.statement.Delete;
import com.k2view.fabric.parser.statement.Insert;
import com.k2view.fabric.parser.statement.Statement;
import com.k2view.fabric.parser.statement.update.UpdateTable;


import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;
import static com.k2view.cdbms.shared.user.UserCode.fabric;
import static com.k2view.cdbms.shared.user.UserCode.getTranslationsData;
import static com.k2view.cdbms.usercode.common.IIDF.SharedLogic.fnIIDFGetTablePK;
import static com.k2view.cdbms.usercode.common.IIDF.SharedLogic.fnIIDFGetTablesCoInfo;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class SharedLogic {


	@desc("JOB for running the IIDF tests.\r\n" +
			"Uses translations trnMainAutoIIDFTest\r\n" +
			"trnDetailAutoIIDFTest")
	@type(UserJob)
	public static void fnAUTOIIDTests() throws Exception {
		Map<String, Map<String, String>> mainIIDTestList = getTranslationsData("trnMainAutoIIDFTest");
		String createKeySpace = "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = %s AND durable_writes = true;";
		String createStmt = "CREATE TABLE if not exists %s (test_id int ,test_step int, test_result text,expected_json text, actual_json text, PRIMARY KEY (test_id,test_step)) WITH CLUSTERING ORDER BY (test_step ASC)";
		String createMainStmt = "CREATE TABLE if not exists %s (test_id int , test_description text, test_result text, time_execution_ms text, PRIMARY KEY (test_id))";
		boolean create = true;
		SimpleDateFormat formatter = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss");
		Date date = new Date();
		String table = "k2iidf_test.iidf_auto_detail_results_" + formatter.format(date);
		String mainTable = "k2iidf_test.iidf_auto_main_results_" + formatter.format(date);
		boolean display = true;
		try{
			for (String seq : mainIIDTestList.keySet()) {
				Map<String, String> map = mainIIDTestList.get(seq);
				String isActive = "" + map.get("ACTIVE");
				if (!isActive.equalsIgnoreCase("TRUE")) {
			            continue;
			        }
				if(create) {
					db("dbCassandra").execute(String.format(createKeySpace, "k2iidf_test", IifProperties.getInstance().getReplicationOptions()));
					db("dbCassandra").execute(String.format(createStmt, table));
					db("dbCassandra").execute(String.format(createMainStmt, mainTable));
				}
				create=false;
				IIDFAutoTestInstance iidCurrTest = new IIDFAutoTestInstance(Integer.parseInt(seq), map.get("TEST_DESCRIPTION"), table, mainTable);
				iidCurrTest.runSimulation();
			}
		}catch(Exception e){
			log.error("Error Executing the Job fnAUTOIIDTests ", e);
			display = false;
			failJobNoRetry(e);
		}finally{
			if(display){
				log.info("Main Test Results are written to - " + mainTable);
				log.info("Detail Test Results are written to - " + table);
			}
		}
	}
	
	public static class IIDFAutoTestInstance {
		private int testID = 0;
		private String testDesc = null;
		private String statsTable = null;
		private String statsMainTable = null;
		private long testStart = 0L;
		private boolean testPass = true;
		protected static Logger log = LoggerFactory.getLogger(IIDFAutoTestInstance.class.getName());
		private Map<String, String> trnDtlList = null;
		private final int lastTest = 100;
		private String insertStmt = "INSERT INTO %s (test_id,test_step,test_result,expected_json,actual_json) VALUES (?,?,?,?,?)  IF NOT EXISTS" ;
		private String insertMainStmt = "INSERT INTO %s (test_id,test_description,test_result,time_execution_ms) VALUES (?,?,?,?)" ;
		private String getLU = null;
	

		public IIDFAutoTestInstance(int testID, String testDesc, String statsTable, String statsMainTable){
			this.testID = testID;
			this.testDesc = testDesc;
			this.statsTable = statsTable;
			this.statsMainTable = statsMainTable;
			this.testStart = System.currentTimeMillis();
		}

		public void runSimulation() throws Exception{
			boolean conTest = false;
			JSONObject failedJson = new JSONObject();
			int testStep = 0;
			
			try{
				for (int i = 1; i<lastTest ; ++i){
					this.trnDtlList = getTranslationValues("trnDetailAutoIIDFTest",new Object[]{testID,i});
					
					if(!"TRUE".equalsIgnoreCase("" + trnDtlList.get("ACTIVE"))) continue;
					
					if ("0".equals(trnDtlList.get("TEST_STEP_TYPE")) || trnDtlList.get("TEST_STEP_TYPE") == null) break;

					String step = trnDtlList.get("TEST_STEP_TYPE").trim();							
					if(!Arrays.asList("GET", "PRODUCER", "SOURCE_QUERY", "TARGET_QUERY").contains(step)) {
						testPass = false;
						conTest = false;
						testStep = i;
						failedJson.put("FAILED",step);
						log.error("Not a Valid step type in trn trnDetailAutoIIDFTest for TEST "+ testID + " STEP " + i);
						break;
					}
					
					conTest = true;
										
					if ("GET".equals(step)){
						conTest = fetchInstance(trnDtlList.get("TEST_COMMAND").split(";")[0].trim(), trnDtlList.get("TEST_COMMAND").split(";")[1].trim());
						this.getLU = trnDtlList.get("TEST_COMMAND").split(";")[0].trim();
					}
						
					if ("PRODUCER".equals(step)){
						try{
							conTest = kafkaProduce(trnDtlList.get("TEST_COMMAND").split(";")[0].trim(), trnDtlList.get("TEST_COMMAND").split(";")[1].trim(),Boolean.parseBoolean(trnDtlList.get("TEST_COMMAND").split(";")[2].trim()));
							if(conTest) Thread.sleep(10000);
						}catch(Exception e){
							log.error("Failed to PRODUCE KAFKA Transaction in trn trnDetailAutoIIDFTest for TEST "+ testID + " STEP " + i);
							conTest = false;
						}
					}			
					
					if ("SOURCE_QUERY".equals(step)){
						try{
							conTest = sourceQuery(trnDtlList.get("TEST_COMMAND").split(";")[0].trim(), trnDtlList.get("TEST_COMMAND").split(";")[1].trim(),Boolean.parseBoolean(trnDtlList.get("TEST_COMMAND").split(";")[2].trim()),i,Boolean.parseBoolean(trnDtlList.get("TEST_COMMAND").split(";")[3].trim()));	
							if(conTest && Boolean.parseBoolean(trnDtlList.get("TEST_COMMAND").split(";")[2].trim())) Thread.sleep(10000);
						}catch(Exception e){
							log.error("Failed to invoke Source|KAFKA Trx in trn trnDetailAutoIIDFTest for TEST "+ testID + " STEP " + i);
							conTest = false;
						}
					}
					
					if("TARGET_QUERY".equals(step)){
						JSONObject expectJson = new JSONObject();
						if (trnDtlList.get("EXPECTED_RESULT") != null && trnDtlList.get("EXPECTED_RESULT").trim().length() > 0)
							conTest = fetchTargetQuery(trnDtlList.get("TEST_COMMAND").split(";")[0].trim(),trnDtlList.get("TEST_COMMAND").split(";")[1].trim(),i,new JSONArray(trnDtlList.get("EXPECTED_RESULT")));			
						else
							conTest = fetchTargetQuery(trnDtlList.get("TEST_COMMAND").split(";")[0].trim(),trnDtlList.get("TEST_COMMAND").split(";")[1].trim(),i,null);
						
						if(!conTest && testPass) testPass =  false;
						conTest = true;
					}	
					
					if (!conTest) {
						testPass = false;
						testStep = i;
						failedJson.put("FAILED",step);
						break;
					}
				}
			}finally{
				if(testPass && conTest) db("dbCassandra").execute(String.format(insertMainStmt,statsMainTable), new Object[]{testID,testDesc,"PASS","" + (System.currentTimeMillis() - testStart)});
				else {
					db("dbCassandra").execute(String.format(insertMainStmt,statsMainTable), new Object[]{testID,testDesc,"FAIL","" + (System.currentTimeMillis() - testStart)});
					if(!conTest) db("dbCassandra").execute(String.format(insertStmt,statsTable), new Object[]{testID,testStep,"FAILED",null,failedJson});
					}
				}
		}

		public boolean fetchInstance(String luName, String IID) {
	
			try {
				fabric().execute("SET SYNC ON");
				fabric().execute("GET ?.?", luName, IID);
			} catch (Exception e) {
				log.warn(String.format("Get 2 Failed on Fabric for TEST %s on LU %s , IID %s.",testID,luName,IID) + e);
				if(!e.getMessage().contains("Can't get instance from black list")){
					return false;
				}
			}
			return true;
		}
		
		public boolean sourceQuery(String dbInterface, String sql, boolean produceInd, int testStep,boolean no_before) {
			try {
				DBExecute(dbInterface, sql,null);
				if(produceInd &&  this.getLU != null) {
					return kafkaProduce(sql, this.getLU,no_before);
				}else if(produceInd){
					String lu = null;
					try {
						lu = getTranslationValues("trnDetailAutoIIDFTest", new Object[]{testID, testStep}).get("TEST_COMMAND").split(";")[4].trim();
					}catch(IndexOutOfBoundsException ie){
						log.error(String.format("LU NAME Mandatory for Testid %s and Step %s.",testID,testStep) + ie);
						return false;
					}
					return kafkaProduce(sql,lu, no_before);
					}
			}catch(Exception e){
				log.error(String.format("Source Query Failed on Interface %s for TEST %s.",dbInterface,testID) + e);
				return false;
			}
			
			return true;
		}
	
		public boolean fetchTargetQuery(String targetDB, String sql, int testStep, JSONArray expectJson) throws Exception{
			boolean status = true;
			try {
				JSONArray resArray = new JSONArray();
				boolean result = false;
				List<Object> params = new LinkedList<Object>(Arrays.asList(testID,testStep));
				Db targetSrc = db(targetDB);
				boolean dataChange = false;
				if(!"fabric".equals(targetDB)) dataChange = isDelta(sql);
				
				try (Db.Rows rs = targetSrc.fetch(sql)) {
					if(!dataChange){
						List<String> columnNames = rs.getColumnNames();
						for (Db.Row row : rs) {
							JSONObject rowJSon = new JSONObject();
							for (String column : columnNames) {
								if("COUNT".equals(column.toUpperCase())){
									rowJSon.put(column.toUpperCase(), Integer.parseInt("" + row.get(column)));
								}else{
									rowJSon.put(column.toUpperCase(), row.get(column));
								}
							}
							resArray.put(rowJSon);
						}
					}else {
						for(Db.Row row : rs){
							JsonObject dcObj = new com.google.gson.JsonParser().parse(row.cell(0) + "").getAsJsonObject();
							JSONObject dcJson = new JSONObject(dcObj.toString());
							resArray.put(dcJson);
							}
						}
				}
										
				if(expectJson != null){
					result = validateJson(expectJson, resArray);
					if (result)
						params.addAll(Arrays.asList("PASS",expectJson.toString(),resArray.toString()));
					else{
						status = false;
						params.addAll(Arrays.asList("FAIL",expectJson.toString(),resArray.toString()));	
						}
					}
				else
					params.addAll(Arrays.asList("PASS","NA",resArray.toString()));		
				
				db("dbCassandra").execute(String.format(insertStmt,statsTable), params.toArray());
				return status;
			}catch (Exception e){
				log.error(String.format("Target Query Failed on LU for TEST %s on TEST_STEP %s .",testID,testStep) + e);
				db("dbCassandra").execute(String.format(insertStmt,statsTable), new Object[]{testID,testStep,"ERROR",null,null});
				return false;
			}
			
		}
		
		public boolean validateJson(JSONArray sourceFetch, JSONArray targetFetch){
			try{
				JSONCompareResult result =  JSONCompare.compareJSON(sourceFetch, targetFetch, JSONCompareMode.LENIENT);
				if(Integer.parseInt("" + result.toString().length()) > 0) 
					return false;
				
				return true;
				}catch(Exception e){
					log.error("JSON Exception While Validating test " + testID + ". " + e);
					return false;
					}
			}
		
		public boolean isDelta(String sql) throws Exception{
			final java.util.regex.Pattern patternSelect = java.util.regex.Pattern.compile("(?i)^select(.*)");
			Statement sqlStmt = null;
			java.util.regex.Matcher matcher = null;
			boolean match = false;
			
			try {
			    sqlStmt = new CCJSqlParserManager().parse(new StringReader(sql));
			} catch (JSQLParserException e) {
			    //log.error("" + e);
				return false;
				//throw e;			   
			}
			
			matcher = patternSelect.matcher(sql);
			if (matcher.find()) {
				List<String> list = Arrays.asList("_delta", "_orphans", "solo");
				List<String> coulmnlist = Arrays.asList("count");
				Select selStmt = (Select) sqlStmt;
				PlainSelect plainSelect = (PlainSelect) selStmt.getSelectBody();
				List selectItems = plainSelect.getSelectItems();
				match = coulmnlist.stream().anyMatch(s -> (""+selectItems).toLowerCase().contains(s));
				if(match) 
					return false;
				
				Set<String> tableList = new SqlStatementAnalyzer(selStmt).tables();
				if(!tableList.isEmpty())
					match = list.stream().anyMatch(s -> (""+tableList).toLowerCase().contains(s));
			}
			return match;
		}
	
		public boolean kafkaProduce(String sql_stmt, String lu_name, boolean no_before) throws Exception {
			final java.util.regex.Pattern patternInsert = java.util.regex.Pattern.compile("(?i)^insert(.*)");
			final java.util.regex.Pattern patternUpdate = java.util.regex.Pattern.compile("(?i)^update(.*)");
			final java.util.regex.Pattern patternDelete = java.util.regex.Pattern.compile("(?i)^delete(.*)");
			String LUTableNotFound = "Failed getting lu table name based on source schema and table names!, LU Name:%s, Source Schema Name:%s, Source Table Name:%s";
			final java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
			Statement sqlStmt = null;
			java.util.regex.Matcher matcher = null;
			String sourceTableName = null;
			StringBuilder messageKey = new StringBuilder();
			
			try {
			    sqlStmt = new CCJSqlParserManager().parse(new StringReader(sql_stmt));
			} catch (JSQLParserException e) {
			    log.error("" + e);
			    return false;
			    //return "Failed to parse SQL statement, Please check logs! - " + sql_stmt;
			}
						
			JSONObject IIDJSon = new JSONObject();
			IIDJSon.put("op_ts", clsDateFormat.format(new java.util.Date()));
	
			IIDJSon.put("current_ts", clsDateFormat.format(new java.util.Date()).replace(" ", "T"));
			IIDJSon.put("pos", "00000000020030806864");
			matcher = patternInsert.matcher(sql_stmt);
			
			if (matcher.find()) {
			    //Insert Statement
			    Insert insStmt = (Insert) sqlStmt;
			
			    if (insStmt.getTable().getSchemaName() == null) {
			        throw new Exception("Schema name in statement is mandatory!");
			    }
				
			    IIDJSon.put("op_type", "I");
			
			    sourceTableName = insStmt.getTable().getSchemaName() + "." + insStmt.getTable().getName();
			
			    String luTableName = null;
			    luTableName = getLUTableName(lu_name, insStmt.getTable().getSchemaName(), insStmt.getTable().getName());
			    		
			    if (luTableName == null) {
			        throw new RuntimeException(String.format(LUTableNotFound, lu_name, insStmt.getTable().getSchemaName(), insStmt.getTable().getName()));
			    }
			
			    Map<String, String> luTableColumnsMap = (Map<String, String>) fnIIDFGetTablesCoInfo(luTableName, lu_name);
			    String[] luTablePKColumns = (String[]) fnIIDFGetTablePK(luTableName, lu_name);
			
			    setPrimaryKeys(luTablePKColumns, IIDJSon);
			
			    JSONObject after = setMessageAfter(((ExpressionList) insStmt.getItemsList()).getExpressions(), insStmt.getColumns(), luTableColumnsMap, IIDJSon, Arrays.asList(luTablePKColumns), messageKey);
			    validatePKExistsInWhere(luTablePKColumns, after);
			} else {
			    matcher = patternUpdate.matcher(sql_stmt);
			    if (matcher.find()) {
			        //Update Statement
			        UpdateTable upStmt = (UpdateTable) sqlStmt;
			
			        if (upStmt.getTable().getSchemaName() == null) {
			            throw new Exception("Schema name in statement is mandatory!");
			        }

			
			        IIDJSon.put("op_type", "U");
			
			        sourceTableName = upStmt.getTable().getSchemaName() + "." + upStmt.getTable().getName();
			
			        String luTableName = null;
		            luTableName = getLUTableName(lu_name, upStmt.getTable().getSchemaName(), upStmt.getTable().getName());
			        
			        if (luTableName == null) {
			            throw new RuntimeException(String.format(LUTableNotFound, lu_name, upStmt.getTable().getSchemaName(), upStmt.getTable().getName()));
			        }
			        Map<String, String> luTableColumnsMap = (Map<String, String>) fnIIDFGetTablesCoInfo(luTableName, lu_name);
			        String[] luTablePKColumns = (String[]) fnIIDFGetTablePK(luTableName, lu_name);
			        setPrimaryKeys(luTablePKColumns, IIDJSon);
			
			        JSONObject before = setMessageBefore(luTableColumnsMap, upStmt.getWhere().toString().split("(?i)( and )"), IIDJSon,Arrays.asList(luTablePKColumns), messageKey);

					if (no_before) {
					//if (no_before != null && no_before) {
		            	IIDJSon.remove("before");
					}
	
			        JSONObject after = setMessageAfter(upStmt.getExpressions(), upStmt.getColumns(), luTableColumnsMap, IIDJSon, null, null);
			        setPrimaryKeysForUpdate(after, before, luTablePKColumns);
			        validatePKExistsInWhere(luTablePKColumns, before);		
			    } else {
			        //Delete Statement
			        matcher = patternDelete.matcher(sql_stmt);
			        if (matcher.find()) {
			            Delete delStmt = (Delete) sqlStmt;
			
			            if (delStmt.getTable().getSchemaName() == null) {
			                throw new Exception("Schema name in statement is mandatory!");
			            }

			            IIDJSon.put("op_type", "D");
			
			            sourceTableName = delStmt.getTable().getSchemaName() + "." + delStmt.getTable().getName();
			
			            String luTableName = null;
			            luTableName = getLUTableName(lu_name, delStmt.getTable().getSchemaName(), delStmt.getTable().getName());
			            
			            if (luTableName == null) {
			                throw new RuntimeException(String.format(LUTableNotFound, lu_name, delStmt.getTable().getSchemaName(), delStmt.getTable().getName()));
			            }
			
			            Map<String, String> luTableColumnsMap = (Map<String, String>) fnIIDFGetTablesCoInfo(luTableName, lu_name);
			            String[] luTablePKColumns = (String[]) fnIIDFGetTablePK(luTableName, lu_name);
			
			            setPrimaryKeys(luTablePKColumns, IIDJSon);
			
			            JSONObject before = setMessageBefore(luTableColumnsMap, delStmt.getWhere().toString().split("(?i)( and )"), IIDJSon, Arrays.asList(luTablePKColumns), messageKey);
			            validatePKExistsInWhere(luTablePKColumns, before);
			        }
			    }
			}
			
			IIDJSon.put("table", sourceTableName.toUpperCase());
			String topicName = "IDfinder." + sourceTableName.toUpperCase();
			
//			Properties props = new Properties();
//		    com.k2view.cdbms.usercode.common.IIDF.SharedLogic.UserKafkaProperties(props);
//		    try (Producer<String, JSONObject> producer = new KafkaProducer<>(props)) {
//		        producer.send(new ProducerRecord(topicName, null, null, IIDJSon.toString())).get();
//		    } catch (Exception e) {
//		        throw e;
//		    }
			
			IIDFProducerSingleton.getInstance().send(topicName, messageKey.toString(), IIDJSon.toString());
		
			return true;
		
			}
			
		private String getLUTableName(String luName, String sourceSchemaName, String sourceTableName) {
			LUType luT = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
			if (luT == null) {
				throw new RuntimeException(String.format("LU Name %s was not found!", luName));
			}
	
			LudbObject rootLUTable = luT.getRootObject();
			return findLUTable(rootLUTable, sourceSchemaName, sourceTableName);
		}
	
		private static String findLUTable(LudbObject rootLUTable, String sourceSchemaName, String sourceTableName) {
			String LUTableName = null;
			for (LudbObject luTable : rootLUTable.childObjects) {
				List<TablePopulationObject> idfinderProp = ((TableObject) luTable).getEnabledTablePopulationObjects();
				if (idfinderProp != null && idfinderProp.size() > 0 && idfinderProp.get(0).iidFinderProp.sourceSchema.equalsIgnoreCase(sourceSchemaName) && idfinderProp.get(0).iidFinderProp.sourceTable.equalsIgnoreCase(sourceTableName)) {
				    return luTable.k2StudioObjectName;
				} else {
				    LUTableName = findLUTable(luTable, sourceSchemaName, sourceTableName);
				    if (LUTableName != null) return LUTableName;
					}
				}
			return LUTableName;
		}
	
		private void setPrimaryKeys(String[] luTablePKColumns, JSONObject IIDJSon) {
			JSONArray PK = new JSONArray();
			for (String pkCul : luTablePKColumns) {
				PK.put(pkCul);
			}
			IIDJSon.put("primary_keys", PK);
		}
	
		private JSONObject setMessageAfter(List<Expression> statementColumnsValues, List<Column> statementColumnsName, Map<String, String> luTableColumnsMap, JSONObject IIDJSon, List<String> tableKeys, StringBuilder messageKey) {
			JSONObject after = new JSONObject();
			int i = 0;
			for (Expression x : statementColumnsValues) {
				String columnName = statementColumnsName.get(i).getColumnName();
				String columnValue = (x + "");
				
				setMessageKey(tableKeys, messageKey, columnName, columnValue);
	
				if (luTableColumnsMap.get(columnName.toUpperCase()).equals("TEXT")) {
					String textVal = columnValue.replaceAll("^'|'$", "");
					after.put(columnName.toUpperCase(), textVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("INTEGER") && columnValue.length() <= 11) {
					int intVal = 0;
					try {
						intVal = Integer.parseInt(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Integer Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					after.put(columnName.toUpperCase(), intVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("INTEGER") && columnValue.length() > 11) {
					long intVal = 0;
					try {
						intVal = Long.parseLong(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Long Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					after.put(columnName.toUpperCase(), intVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("REAL")) {
					double doubeValue = 0;
					try {
						doubeValue = Double.parseDouble(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Double Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					after.put(columnName.toUpperCase(), doubeValue);
				}
				i++;
			}
			IIDJSon.put("after", after);
			return after;
		}
	
		private static void validatePKExistsInWhere(String[] pkCuls, JSONObject before) throws Exception {
			for (String keyColumn : pkCuls) {
				if (!before.has(keyColumn)) {
					throw new Exception("All primary key columns must be part of where!");
				}
			}
		}
		
		private static void setMessageKey(List<String> tableKeys, StringBuilder messageKey, String columnName, String columnValue) {
        	if (tableKeys != null && tableKeys.contains(columnName.toUpperCase())) messageKey.append(columnValue);
		}
	
		private JSONObject setMessageBefore(Map<String, String> luTableColumnsMap, String[] statementColumnsNdValues, JSONObject IIDJSon, List<String> tableKeys, StringBuilder messageKey) {
			JSONObject before = new JSONObject();
			for (String culNdVal : statementColumnsNdValues) {
				String columnName = culNdVal.split("=")[0].trim();
				String columnValue = culNdVal.split("=")[1].trim();
				
				if (tableKeys != null) setMessageKey(tableKeys, messageKey, columnName, columnValue);
	
				if (luTableColumnsMap.get(columnName.toUpperCase()).equals("TEXT")) {
					String textVal = columnValue.replaceAll("^'|'$", "");
					before.put(columnName.toUpperCase(), textVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("INTEGER") && columnValue.length() <= 11) {
					int intVal = 0;
					try {
						intVal = Integer.parseInt(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Integer Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					before.put(columnName.toUpperCase(), intVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("INTEGER") && columnValue.length() > 11) {
					long intVal = 0;
					try {
						intVal = Long.parseLong(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Long Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					before.put(columnName.toUpperCase(), intVal);
				} else if (luTableColumnsMap.get(columnName.toUpperCase()).equals("REAL")) {
					double doubeValue = 0;
					try {
						doubeValue = Double.parseDouble(columnValue);
					} catch (Exception e) {
						log.error("" + e);
						throw new RuntimeException("Failed To Parse Double Value For Column " + columnName + ", Value Found:" + columnValue);
					}
					before.put(columnName.toUpperCase(), doubeValue);
				}
			}
			IIDJSon.put("before", before);
			return before;
		}
	
		private void setPrimaryKeysForUpdate(JSONObject after, JSONObject before, String[] luTablePKColumns) {
			List<String> pkList = Arrays.asList(luTablePKColumns);
			Iterator<String> beforeKeys = before.keys();
			while (beforeKeys.hasNext()) {
				String key = beforeKeys.next();
				if (pkList.contains(key.toUpperCase()) && !after.has(key)) {
					after.put(key, before.get(key));
				}
			}
		}
	}

}
