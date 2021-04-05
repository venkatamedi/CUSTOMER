package com.k2view.cdbms.usercode.lu.CUSTOMER;

import java.text.DecimalFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import java.util.regex.Pattern;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.k2view.cdbms.shared.user.UserCode.*;

public class MYTopicConsumer {
	protected static Logger log = LoggerFactory.getLogger(MYTopicConsumer.class.getName());
	boolean isPolling = true;
	public KafkaConsumer<String, JSONObject> consumer;

	public MYTopicConsumer(String groupId, String topicName) throws Exception {
		this.consumer = new KafkaConsumer<>(initProps(groupId));
		startPolling(topicName);
    }
	public void startPolling(String topicName) throws InterruptedException, Exception {
		 this.consumer.subscribe(Pattern.compile(topicName), new NoOpConsumerRebalanceListener());
		 try {
			 JSONObject currentRecord = null;
            while (isPolling) {
				 ConsumerRecords<String, JSONObject> records = consumer.poll(1000);
				 for(ConsumerRecord<String, JSONObject> record : records){
						currentRecord = record.value();
						log.info("MESSAGE: "+ currentRecord.toString());
					    Object iid = currentRecord.getString("IID");
						    if (iid != null) {
								try {
									fabric().execute("GET CUSTOMER.?", iid);
									log.info("GET COMPLETED SUCCESSFULLY "+ iid);
								}catch (Exception e){
									log.error("GET FAILED " + iid + ", Exception:", e);
								}
							}else{
						    	throw new Exception("FAILED TO RETRIEVE IID FROM THE JSON"+ currentRecord);
							}
				 }
			}
 		}finally {
            consumer.unsubscribe();
            log.warn("MYTopicConsumer: Stopped polling");
			if(consumer != null)consumer.close();
        }
	}
	
	private Properties initProps(String groupId) {
		Properties props = new Properties();
		props.put("group.id", groupId == null ? "IIDFinderGroupId" : groupId);
		props.put("bootstrap.servers", IifProperties.getInstance().getKafkaBootsrapServers());
		props.put("enable.auto.commit", "false");
		props.put("auto.offset.reset", "earliest");
		props.put("max.poll.records", 100);
		props.put("session.timeout.ms", 30000);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "com.k2view.cdbms.kafka.JSONObjectDeserializer");
		if (com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getBoolean("SSL_ENABLED", false)) {
            props.put("security.protocol", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SECURITY_PROTOCOL", null));
            props.put("ssl.truststore.location", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("TRUSTSTORE_LOCATION", null));
            props.put("ssl.truststore.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("TRUSTSTORE_PASSWORD", null));
            props.put("ssl.keystore.location", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEYSTORE_LOCATION", null));
            props.put("ssl.keystore.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEYSTORE_PASSWORD", null));
            props.put("ssl.key.password", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("KEY_PASSWORD", null));
            props.setProperty("ssl.endpoint.identification.algorithm", "");
            props.put("ssl.endpoint.identification.algorithm", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("ENDPOINT_IDENTIFICATION_ALGORITHM", null));
            props.put("ssl.cipher.suites", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_CIPHER_SUITES", null));
            props.put("ssl.enabled.protocols", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_ENABLED_PROTOCOLS", null));
            props.put("ssl.truststore.type", com.k2view.cdbms.shared.IifProperties.getInstance().getProp().getString("SSL_TRUSTSTORE_TYPE", null));
        }
		return props;
    }
}
