/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Test;

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
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import javax.servlet.ServletInputStream;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@webService(path = "", verb = {MethodType.GET, MethodType.POST, MethodType.PUT, MethodType.DELETE}, version = "1", isRaw = false, produce = {Produce.XML, Produce.JSON})
	public static String wsUsePOST() throws Exception {
		StringBuilder reqBody = new StringBuilder();
		ServletInputStream sis = request().getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(sis));
		String str = "";
		if (sis !=null) {
			log.info("SFSFSF");
		}
		while ((str = br.readLine()) != null) {
			reqBody.append(str);
		}
		if (StringUtils.isNotBlank(reqBody.toString())) {;
			JSONObject jsonObjectRoot = new JSONObject(reqBody.toString());
			log.info(jsonObjectRoot.toString());
		}
		return "SUCCESS";
	}

	
	

	
}
