/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Assignment;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import com.k2view.cdbms.usercode.lu.k2_ws.AdvancedJavaAssignment.GetAvgPaymentDetails;
import com.k2view.fabric.api.endpoint.Endpoint.*;
import org.elasticsearch.index.engine.Engine;
import org.json.JSONObject;

import static com.k2view.cdbms.lut.FunctionDef.functionContext;
import static com.k2view.cdbms.usercode.lu.k2_ws.AdvancedJavaAssignment.GetAvgPaymentDetails.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@webService(path = "", verb = {MethodType.GET, MethodType.POST, MethodType.PUT, MethodType.DELETE}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	public static String GetAllCustPaymentDetails() throws Exception {
		long start_time = System.currentTimeMillis();
		int i = 0;
		double payment_avg= 0.0;
		Map<String, Object> custAvg = new HashMap();
		ExecutorService es= Executors.newFixedThreadPool(1);
		List<String> allCustAvg = new ArrayList<String>();
		try(Db.Rows allcust = db("dbCassandra").fetch("select id from k2view_customer_6_4_iidf_tests.entity limit 100");) {
			for(Db.Row forEachCust : allcust) {
				
		//		[1:03 PM] Nir Kehat
		    
		//openFabricSession
		
				i++;
				GetAvgPaymentDetails getAvgP = new GetAvgPaymentDetails(forEachCust.cell(0),openFabricSession("dbFabric"));
				es.submit(getAvgP);
				Thread.sleep(100);
				JSONObject obj = new JSONObject();
				obj.put("CUSTOMER_ID", ""+forEachCust.cell(0));
				String a = ""+getAvgP.getPaymentAvg();
				obj.put("AVG_AMOUNT", a);
				if("".equals(""+obj.get("AVG_AMOUNT"))){
					throw new Exception("FAILED TO PERFORM GET " + forEachCust.cell(0));	
				}
				allCustAvg.add(obj.toString());
			}
			//Thread.sleep(1000);
			es.shutdown();
			log.info("TOTAL_TIME_FOR_WS_GetAllCustPaymentDetails:" +(System.currentTimeMillis() - start_time));
			log.info("ALLCUSTAVG:"+allCustAvg.toString());
			return allCustAvg.toString();
		}
		
		
		      
		
		
		/*
		{"CUSTOMER_ID":"ID","AVG_AMOUNT":"AMOUNT"},
		{"CUSTOMER_ID":"ID","AVG_AMOUNT":"AMOUNT"},
		{"CUSTOMER_ID":"ID","AVG_AMOUNT":"AMOUNT"},
		
		{
		  "1797204212": "520.13",
		  "1865563589": "497.33",
		  "2247033317": "487.00",
		  "2961755996": "353.67",
		  "2997155382": "528.75",
		  "3224511357": "590.75",
		  "3327040408": "0",
		  "3558984919": "417.50",
		  "4010105616": "452.13",
		  "4076867294": "541.00",
		  "5753808223": "308.33",
		  "4555993275": "633.29",
		  "6574121726": "511.78",
		  "8661218973": "748.75",
		  "0263391730": "0",
		  "8690891749": "463.40",
		  "8269067010": "544.00",
		  "4705834596": "734.50",
		  "9778466437": "602.50",
		  "6407495759": "0",
		  "0288415160": "0",
		  "0777001597": "925.00",
		  "8797021104": "0"
		}
		*/
	}


	
	

	
}
