/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.AdvancedJavaAssignment;

import java.text.DecimalFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import com.k2view.fabric.api.endpoint.Endpoint.*;
import org.elasticsearch.index.engine.Engine;

import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.user.UserCode.log;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Connection;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class  GetAvgPaymentDetails implements Runnable{
	private static Object instanceId = null;
	private static String paymentAvg = "";
	private static UserCodeDelegate ucd;


	public GetAvgPaymentDetails(Object iid,  Connection connection  ){
		this.instanceId = iid;
		this.ucd = (UserCodeDelegate) connection;
	}
	public void run(){
		try {
			this.ucd.fabric().execute("Set sync on");
			this.ucd.fabric().execute("GET CUSTOMER.?", this.instanceId);
			Object avgP = this.ucd.fabric().fetch("select avg(amount) from payment").firstValue();
			if (avgP != null) {
				double number = Double.parseDouble(""+avgP);
				paymentAvg = ""+ String.format("%.2f", number);
				//paymentAvg = Math.round(paymentAvg * 100D) / 100D;
			}else{
				paymentAvg = "0";
			}
			
			log.info("CURRENT_THREAD_IN_METHOD: "+Thread.currentThread().getId() + ", IID: "+ this.instanceId +", PAYMENT_AVG: "+paymentAvg);
		} catch (Exception e) {
			log.error("GET FAILED "+ this.instanceId, e);
		}
	}
	public static String getPaymentAvg(){
		return paymentAvg;
	}
}
