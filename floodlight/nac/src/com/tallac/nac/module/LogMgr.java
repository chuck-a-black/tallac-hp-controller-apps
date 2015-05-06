/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.module;

import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.ClientMgr.Client;

public class LogMgr {
	
	private static final LogMgr INSTANCE = new LogMgr();
	private static final Logger LOG = LoggerFactory.getLogger(LogMgr.class);

    /** 
     * Class responsible for handling client information
     */
    public static class LogRecord 
    {
        private Date   mDate;
        private Client mClient;
        private String mEvent;

        public LogRecord(Client client, String event) 
        {
            mClient = client;
            mEvent = event;
            mDate = new Date();
        }

        public Client getClient() 
        {
            return mClient;
        }

        public String getEvent() 
        {
            return mEvent;
        }

        public Date getDate() 
        {
            return mDate;
        }

        @Override
        public String toString() {
            return "LogRecord [mDate=" + mDate + ", mClient=" + mClient
                    + ", mEvent=" + mEvent + "]";
        }
    }
    
	private ArrayList<LogRecord> mLogRecords;

	private LogMgr() {
		// private constructor - prevent external instantiation
		mLogRecords = new ArrayList<LogRecord>();
	}

	public static LogMgr getInstance() {
		return INSTANCE;
	}

	public ArrayList<LogRecord> getLog()
	{
        return new ArrayList<LogRecord>(mLogRecords);
	}

    public void log(Client client, String event) {
        LogRecord logRecord = new LogRecord(client, event);
        LOG.trace("Record {} has been added to the Tallac NAC logs", logRecord);
        mLogRecords.add(logRecord);
    }
}
