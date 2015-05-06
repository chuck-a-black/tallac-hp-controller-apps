/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */
package com.tallac.nac;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Tallac NAC software revision version number.
 */
public final class Version
{
    private static final String VERSION_PROPERTIES = "/version.properties";

    // Values are read from properties file
    private static int major = -1;
    private static int minor = -1;
    private static int patch = -1;
    private static int build = -1;

    static
    {
        try
        {
            parseVersionProperties();
        }
        catch (final Throwable e)
        {
            throw new InternalError("Failed to load version information");
        }
    }

    private static void parseVersionProperties() throws IOException
    {
        final InputStream is;
        final Properties properties = new Properties();
        final String propertiesFile = VERSION_PROPERTIES;
        is = Properties.class.getResourceAsStream(propertiesFile);
        properties.load(is);

        final Enumeration<?> e = properties.propertyNames();
        while (e.hasMoreElements())
        {
            final String key = (String) e.nextElement();
            final String value = properties.getProperty(key);

            if (key.equals("version.major"))
            {
                major = Integer.parseInt(value);
            }
            else if (key.equals("version.minor"))
            {
                minor = Integer.parseInt(value);
            }
            else if (key.equals("version.patch"))
            {
                patch = Integer.parseInt(value);
            }
            else if (key.equals("version.build"))
            {
                build = Integer.parseInt(value);
            }
        }

        if (major == -1 || minor == -1 || patch == -1 || build == -1)
        {
            throw new InternalError();
        }
    }

    private Version()
    {
        // private constructor - prevent instantiation
    }

    /**
     * Gets the version string.
     *
     * @return the version string
     */
    public static String getVersionString()
    {
        return getMajorNumber() + "." + getMinorNumber() + "." +
               getPatchNumber() + "." + getBuildNumber();
    }

    /**
     * Gets the major version number.
     *
     * @return the major number
     */
    public static int getMajorNumber()
    {
        return major;
    }

    /**
     * Gets the minor version number.
     *
     * @return the minor number
     */
    public static int getMinorNumber()
    {
        return minor;
    }

    /**
     * Gets the patch version number.
     *
     * @return the patch number
     */
    public static int getPatchNumber()
    {
        return patch;
    }

    /**
     * Gets the build version number.
     *
     * @return the build number
     */
    public static int getBuildNumber()
    {
        return build;
    }
}
