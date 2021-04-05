/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.KAFKA_Assignment;

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

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

import com.k2view.fabric.common.Util;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.network.Send;
import org.json.JSONObject;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {

	@out(name = "isNullorEmpty", type = Boolean.class, desc = "")
	public static Boolean k2_isNullorEmptyString(String iStr) throws Exception {
		// Checks if the String is NULL or empty
		//return iStr == null || iStr.isEmpty();

		return (org.apache.commons.lang3.StringUtils.isBlank(iStr)||String.valueOf(iStr).equalsIgnoreCase("null"));
	}
	@webService(path = "", verb = {MethodType.GET, MethodType.POST, MethodType.PUT, MethodType.DELETE}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	public static Object wsProduce(String iid, Boolean SendToProducerIndicator) throws Exception {
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		java.util.Date currentTime = new java.util.Date();
		String currTime = dateFormat.format(currentTime);
		JSONObject produceMsg = new JSONObject();
		produceMsg.put("IID",iid);
		produceMsg.put("current_ts",currTime);
		String msg = "";
		if(SendToProducerIndicator && !k2_isNullorEmptyString(iid)) {
			Producer<String, JSONObject> producer = new KafkaProducer<>(getProducerProp());
			try{
				producer.send(new ProducerRecord("MY_TOPIC", null, iid, produceMsg.toString()), new Callback() {
					@Override
					public void onCompletion(RecordMetadata recordMetadata, Exception e) {
						
						if(e == null){
							//msg  = "Message sent";
						}else{
							//msg=  "Failed to send message";
						}
						//reportUserMessage(msg);
					}
				});
				return msg;
			}finally {
				if (producer != null) producer.close();
				//throw e;
			}
		}else {
			return produceMsg.toString();
		}
	}



		private static Properties getProducerProp() {
			Properties props = new Properties();
			props.put("bootstrap.servers", IifProperties.getInstance().getKafkaBootsrapServers());
			props.put("acks", "all");
			props.put("retries", "5");
			props.put("batch.size", "" + IifProperties.getInstance().getKafkaBatchSize());
			props.put("linger.ms", 1);
			props.put("max.block.ms", "" + IifProperties.getInstance().getKafkaMaxBlockMs());
			props.put("buffer.memory", "" + IifProperties.getInstance().getKafkaBufferMemory());
			props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			Properties sslProps =getSSLProperties();
			props.putAll(sslProps);
			return props;
		}
	private static Properties getSSLProperties() {
		Properties props = new Properties();
		if (com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getBoolean("SSL_ENABLED", false)) {
			appendProperty(props, "security.protocol", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SECURITY_PROTOCOL", null));
			appendProperty(props, "ssl.truststore.location", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("TRUSTSTORE_LOCATION", null));
			appendProperty(props, "ssl.truststore.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("TRUSTSTORE_PASSWORD", null));
			appendProperty(props, "ssl.keystore.location", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEYSTORE_LOCATION", null));
			appendProperty(props, "ssl.keystore.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEYSTORE_PASSWORD", null));
			appendProperty(props, "ssl.key.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEY_PASSWORD", null));
			props.setProperty("ssl.endpoint.identification.algorithm", "");
			appendProperty(props, "ssl.endpoint.identification.algorithm", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("ENDPOINT_IDENTIFICATION_ALGORITHM", null));
			appendProperty(props, "ssl.cipher.suites", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_CIPHER_SUITES", null));
			appendProperty(props, "ssl.enabled.protocols", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_ENABLED_PROTOCOLS", null));
			appendProperty(props, "ssl.truststore.type", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_TRUSTSTORE_TYPE", null));
			return props;
		} else {
			return props;
		}
	}
	private static void appendProperty(Properties p, String key, String value) {
		Objects.requireNonNull(key);
		if (!Util.isEmpty(value)) {
			p.put(key, value);
		}
	}


}
