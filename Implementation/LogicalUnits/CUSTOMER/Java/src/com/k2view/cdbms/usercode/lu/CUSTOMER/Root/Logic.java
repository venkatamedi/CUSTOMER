/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.CUSTOMER.Root;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.CUSTOMER.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.CUSTOMER.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "customer_id", type = Long.class, desc = "")
	@out(name = "ssn", type = String.class, desc = "")
	@out(name = "first_name", type = String.class, desc = "")
	@out(name = "last_name", type = String.class, desc = "")
	public static void fnPop_customer(String input) throws Exception {
		String sql = "SELECT customer_id, ssn, first_name, last_name FROM public.customer";
		db("CRM_DB").fetch(sql).each(row->{
			yield(row.cells());
		});
	}


	@type(RootFunction)
	@out(name = "id", type = Long.class, desc = "")
	@out(name = "name", type = String.class, desc = "")
	public static void fnPop_cust(String input) throws Exception {
		String sql = "SELECT id, name FROM public.cust";
		db("CRM_DB").fetch(sql).each(row->{
			Thread.sleep(20000);
			yield(row.cells());
		});
	}

	
	
	
	
}
