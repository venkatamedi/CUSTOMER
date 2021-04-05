/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Utilities;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.config.TableProperties;
import com.k2view.cdbms.finder.TableProperty;
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

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@webService(path = "", verb = {MethodType.GET, MethodType.POST, MethodType.PUT, MethodType.DELETE}, version = "1", isRaw = false, produce = {Produce.XML, Produce.JSON})
	public static String test() throws Exception {
		Optional<TableProperty> tablePropertes = TableProperties.getInstance().getTablePropertyForLu("customer", "public_sim_name");
		if (tablePropertes.isPresent()) {
		    TableProperty tblProp = tablePropertes.get();
		    return tblProp.getTableKeyspace() + "." + tblProp.getTableName();
		} else {
		    log.warn("Can't find table name!");
		    return "Can't find table name!";
		}
	}

	
	

	
}
