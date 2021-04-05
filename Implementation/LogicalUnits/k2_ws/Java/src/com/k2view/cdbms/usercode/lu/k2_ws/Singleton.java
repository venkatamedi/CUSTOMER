package com.k2view.cdbms.usercode.lu.k2_ws;

import java.time.Instant;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import static com.k2view.cdbms.shared.user.UserCode.log;

public class Singleton {
	private static Singleton singleton = new Singleton();
    private static Cache<String, String> IIDCache;
    public Singleton(){
        this.IIDCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                //.removalListener(MY_LISTENER)
                .build();
    }
    public static Singleton getInstance() {
        return singleton;
    }
    public boolean checksync(UserCodeDelegate ucd, String iid) throws SQLException {

        String IIDExitsInCache = this.IIDCache.getIfPresent(iid);
        if (IIDExitsInCache == null) {
            IIDCache.put(iid, "Y");
            ucd.fabric().execute("SET SYNC ON");
            return true;
        } else {
           ucd.fabric().execute("SET SYNC OFF");
            return false;
        }
    }
}
