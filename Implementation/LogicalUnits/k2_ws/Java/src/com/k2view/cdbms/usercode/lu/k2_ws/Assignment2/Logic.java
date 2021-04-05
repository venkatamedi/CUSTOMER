/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Assignment2;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import com.k2view.fabric.api.endpoint.Endpoint.*;

import static com.k2view.cdbms.lut.FunctionDef.functionContext;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@webService(path = "", verb = {MethodType.GET, MethodType.POST, MethodType.PUT, MethodType.DELETE}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	public static Db.Rows GetAllCustomerCases(String instanceId) throws Exception {
		Singleton.getInstance().checksync(functionContext(), instanceId);
		fabric().execute("GET CUSTOMER.?", instanceId);
		return fabric().fetch("select ACTIVITY_ID,CASE_ID,CASE_DATE,CASE_TYPE,STATUS from CASES");
		
		/*
		[
		  {
		    "ACTIVITY_ID": "3095",
		    "CASE_ID": "1550",
		    "CASE_DATE": "2016-05-16",
		    "CASE_TYPE": "Billing Issue",
		    "STATUS": "Unresolved"
		  },
		  {
		    "ACTIVITY_ID": "3096",
		    "CASE_ID": "1551",
		    "CASE_DATE": "2015-10-23",
		    "CASE_TYPE": "Device Issue",
		    "STATUS": "Open"
		  },
		  {
		    "ACTIVITY_ID": "3097",
		    "CASE_ID": "1552",
		    "CASE_DATE": "2015-06-16",
		    "CASE_TYPE": "Network Issue",
		    "STATUS": "Unresolved"
		  },
		  {
		    "ACTIVITY_ID": "3100",
		    "CASE_ID": "1553",
		    "CASE_DATE": "2016-08-03",
		    "CASE_TYPE": "Billing Issue",
		    "STATUS": "Unresolved"
		  },
		  {
		    "ACTIVITY_ID": "3101",
		    "CASE_ID": "1554",
		    "CASE_DATE": "2015-10-15",
		    "CASE_TYPE": "Billing Issue",
		    "STATUS": "Closed"
		  },
		  {
		    "ACTIVITY_ID": "3102",
		    "CASE_ID": "1555",
		    "CASE_DATE": "2015-04-17",
		    "CASE_TYPE": "Network Issue",
		    "STATUS": "Closed"
		  },
		  {
		    "ACTIVITY_ID": "3103",
		    "CASE_ID": "1556",
		    "CASE_DATE": "2015-07-06",
		    "CASE_TYPE": "Network Issue",
		    "STATUS": "Closed"
		  }
		]
		*/
	}
}
