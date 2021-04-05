/////////////////////////////////////////////////////////////////////////
// Shared Globals
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common;

import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

public class SharedGlobals {

	





	@desc("Indicate for how long to keep data in IIDF_RECENT_UPDATES Table in hours")
	@category("IIDF")
	public static String IIDF_KEEP_HISTORY_TIME = "0";
	@desc("Indicate Project Cassandra interface name")
	@category("IIDF")
	public static final String DB_CASS_NAME = "dbCassandra";
	@desc("Indicate the delay between the source to kafka - in minutes")
	@category("IIDF")
	public static String IIDF_SOURCE_DELAY = "0";

	@desc("Indicate if to run LUDB's source population map, Need to be set per LU")
	@category("IIDF")
	public static String IIDF_EXTRACT_FROM_SOURCE = "true";


	@desc("Indicate the list of  ludb's root table, Need to be set per LU")
	@category("IIDF")
	public static final String IIDF_ROOT_TABLE_NAME = "";


	@desc("If to insertr statistics to IIDF_STATISTICS common table")
	@category("IIDF")
	public static String STATS_ACTIVE = "false";














	@desc("If to insert IID statistics")
	@category("IIDF")
	public static String IID_STATS = "false";

	@desc("If to skip cross IID logic")
	@category("IIDF")
	public static String CROSS_IID_SKIP_LOGIC = "false";

	@desc("If data change process time in ms is bigger then this a warning with details will  be printed to log")
	@category("IIDF")
	public static String DEBUG_LOG_THRESHOLD = "200";

	@desc("The path to Kafka bin folder on fabric's node")
	@category("IIDF")
	public static String PATH_TO_KAFKA_BIN = "";

	@desc("Lus list (comma seperated) to send statistics for")
	@category("IIDF")
	public static String STATS_EMAIL_REPORT_LUS = "";

	@desc("The prefix used for LU Table Kafka topics")
	@category("IIDF")
	public static String IDFINDER_TOPIC_PREFIX = "IidFinder.";
	
	@desc("The group id used for parser get")
	@category("IIDF_PARSER_GET")
	public static String GET_PARSER_GROUP_ID = "fabric_default";

	@desc("Used to slow down the parser get")
	@category("IIDF_PARSER_GET")
	public static String IID_GET_DELAY = "";
	@desc("Time to start parser get")
	@category("IIDF_PARSER_GET")
	public static String GET_JOB_START_TIME = "";
	@desc("Time to stop parser get")
	@category("IIDF_PARSER_GET")
	public static String GET_JOB_STOP_TIME = "";
	@desc("If to run parser get manager")
	@category("IIDF_PARSER_GET")
	public static String GET_JOBS_IND = "";
	@desc("Delay iid process time")
	@category("IIDF_PARSER_GET")
	public static String DELTA_JOB_DELAY_TIME = "";
	@desc("iid sync will be skipped if the previous sync of this Iid occurred in the last X seconds")
	@category("IIDF_PARSER_GET")
	public static String DELTA_JOB_PREV_MESSAGE_DIFF_EPSILON_MS = "";
	@desc("Maximum IIDs to keep in cache")
	@category("IIDF_PARSER_GET")
	public static String DELTA_JOB_MAX_CACHE_SIZE = "";

	@desc("If IDFinder lag is bigger then this, Parser get will be stopoped")
	@category("IIDF_PARSER_GET")
	public static String LAG_THRESHOLD = "";

	@desc("Delay orphan msg process time")
	@category("IIDF_PARSER_ORPHAN")
	public static String ORPHAN_JOB_DELAY_TIME = "";













	


}
