/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.module;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigMgr {
	
	private static final ConfigMgr INSTANCE        = new ConfigMgr();
	private static final Logger    LOG             = LoggerFactory.getLogger(ConfigMgr.class);
    private static final String    NAC_CONFIG_FILE = "nac.properties";

    private PropertiesConfiguration mProperties;
    
	private ConfigMgr() 
	{
        // private constructor - prevent external instantiation
	}

    public void init() 
    {
        try
        {
            mProperties = new PropertiesConfiguration();      // Load NAC properties from file
            mProperties.setDelimiterParsingDisabled( true );  // disable delimiter parsing in each flow
            
            mProperties.load(NAC_CONFIG_FILE);
        }
        catch (ConfigurationException e) {
            LOG.error("Unable to read property file {} {} ", NAC_CONFIG_FILE, e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("unused")
    private void saveProperties() {
        try {
            mProperties.save(NAC_CONFIG_FILE);  //save properties
        }
        catch (ConfigurationException e) {
            LOG.error("Property file could not be saved: " + e.getLocalizedMessage());
        }
    }

	public static ConfigMgr getInstance() {
		return INSTANCE;
	}
	
	public String getString(String key)
	{
        return mProperties.getString(key);
	}
	
    public String[] getStringArray(String key)
    {
        return mProperties.getStringArray(key);
    }

    public short getShort(String key) {
        return mProperties.getShort(key);
    }

}
