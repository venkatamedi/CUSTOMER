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

public class CustomerUpdatesConsumer {
	protected static Logger log = LoggerFactory.getLogger(CustomerUpdatesConsumer.class.getName());
	boolean isPolling = true;

	private static final int FIVE_MIN = 300;
	private static DecimalFormat dF = new DecimalFormat("##.##");
	public KafkaConsumer<String, JSONObject> consumer;

	public CustomerUpdatesConsumer(String groupId, String topicName) throws Exception {
		this.consumer = new KafkaConsumer<>(initProps(groupId));
		startPolling(topicName);
    }
	public void startPolling(String topicName) throws InterruptedException, Exception {
		 this.consumer.subscribe(Pattern.compile(topicName), new NoOpConsumerRebalanceListener());
		 try {
			 JSONObject currentRecord = null;
			 Stats stats = new Stats();
            while (isPolling) {
				 ConsumerRecords<String, JSONObject> records = consumer.poll(1000);
				 for(ConsumerRecord<String, JSONObject> record : records){

						currentRecord = record.value();
					    Object ssn = currentRecord.getJSONObject("after").get("SSN");
						    if (ssn != null) {
								try {
								stats.set_total_get_processed_in_five_min();
								fabric().execute("GET CUSTOMER.?", ssn);
								}catch (Exception e){
									stats.set_total_get_failed_in_five_min();
									log.error("GET is failing" + ssn);
									db("dbCassandra").execute("Insert into " + getLuType().getKeyspaceName() + ".failed_customer (iid, failed_time, failure_reason, full_error_msg) values (?,?,?,?)", ssn, new Timestamp(System.currentTimeMillis()), e.getMessage(), ExceptionUtils.getStackTrace(e));
									//throw e;
								}
							}else{
						    	throw new Exception("FAILED TO RETRIEVE SSN FROM THE MESSAGE"+ currentRecord);
							}

					  if (System.currentTimeMillis() > stats.getFiveMin()) {
			          	 stats.insertStats();
			          }
				 }
			}
 		}finally {
            consumer.unsubscribe();
            log.warn("ConsumerUpdatesConsumer: Stopped polling");
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


	 private static class Stats {
        long total_get_failed;
        long total_get_processed;
        long total_get_processed_in_five_min;
		long total_get_failed_in_five_min;
        long fiveMin;

        private Stats(){
            this.fiveMin = System.currentTimeMillis() + FIVE_MIN;
        }
        private long getFiveMin() {
            return fiveMin;
        }

        private void set_total_get_failed_in_five_min() {
			total_get_failed_in_five_min++;
        }

		 private void set_total_get_failed(long total_get_failed_in_five_min) {
			 total_get_failed += total_get_failed_in_five_min;
		 }

        private void set_total_get_processed(long total_get_processed_in_five_min) {
            total_get_processed += total_get_processed_in_five_min;
        }

        private void set_total_get_processed_in_five_min(){
			total_get_processed_in_five_min++;
        }

        private void insertStats() throws SQLException {

            double processed_avg = (total_get_processed_in_five_min / (FIVE_MIN));
			processed_avg = Double.parseDouble(dF.format(processed_avg));

			set_total_get_processed(total_get_processed_in_five_min);// total number of gets performed from start
			set_total_get_failed(total_get_failed_in_five_min); // total number of gets failed from start
            db("dbCassandra").execute("Insert into " + getLuType().getKeyspaceName() + ".my_consumer_stats (process_name,avg_get_rate, total_get_processed,total_get_processed_in_five_min,total_get_failed, total_get_failed_in_five_min,updated_time ) values (?,?,?,?,?,?,?)",  "CustomerUpdatesConsumer",processed_avg,total_get_processed,total_get_processed_in_five_min,total_get_failed,total_get_failed_in_five_min,new Timestamp(System.currentTimeMillis()));
			fiveMin = System.currentTimeMillis() + (FIVE_MIN * 1000);
            total_get_processed_in_five_min = 0;
			total_get_failed_in_five_min = 0;
        }
    }
}
