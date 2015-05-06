package com.tallac.nac.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.restlet.Response;
import org.restlet.data.Status;

import net.floodlightcontroller.packet.IPv4;

public class TallacUtils {

	/**
	 * Convert string representation of IPv4 address to InetAddress class.
	 */
	public static InetAddress convertIpv4Address(String line)
	{
	    
	    if (line == null || line.isEmpty()) return null;  /* return if line is empty or null */
	    if ((line.length() < 7) & (line.length() > 15)) return null;  /* check max and min length */
	
	    /* check pattern */
	    try
	    {
	        Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
	        Matcher matcher = pattern.matcher(line);
	        if (matcher.matches())
	        {
	            try
	            {
	                return InetAddress.getByName(line);
	            }
	            catch (UnknownHostException e)
	            {
	            }
	        }
	    }
	    catch (PatternSyntaxException ex)
	    {
	    }
	
	    return null;
	}
	
    public static int inetAddressToInt(InetAddress inetAddr)
    {
        return ByteBuffer.wrap(inetAddr.getAddress()).getInt();
    }
    
    public static InetAddress intToInetAddress(int ip)
    {
        InetAddress ipAddr = null;
        try
        {
            ipAddr = InetAddress.getByAddress( IPv4.toIPv4AddressBytes(ip) );
            return ipAddr;
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String intIpToString(int ip)
    {
        InetAddress ipAddr = null;
        try
        {
            ipAddr = InetAddress.getByAddress( IPv4.toIPv4AddressBytes(ip) );
            return ipAddr.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            return "";
        }
    }
    
    public static void setRestApiError(String errStr)
    {
        Response.getCurrent().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, errStr);
    }

}
