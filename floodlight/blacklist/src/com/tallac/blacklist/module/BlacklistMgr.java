package com.tallac.blacklist.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.floodlightcontroller.core.module.FloodlightModuleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BlacklistMgr class responsible for reading and parsing DNS and IPv4
 * configuration files.
 */
public class BlacklistMgr
{
    private static final BlacklistMgr INSTANCE = new BlacklistMgr();
    private static final Logger       LOG      = LoggerFactory.getLogger(BlacklistMgr.class);

    private static final String DNSBlacklistFilename  = "dnsBlacklist.txt";
    private static final String IPv4BlacklistFilename = "ipv4Blacklist.txt";

    //---- Two hashes, one for DNS and one for IP traffic
    HashSet<String>      mDnsBlacklist;
    HashSet<InetAddress> mIpv4Blacklist;

    //---------------------------------------------------------------------------------------------
    private BlacklistMgr()
    {
        // private constructor - prevent external instantiation
        mDnsBlacklist  = new HashSet<String>();
        mIpv4Blacklist = new HashSet<InetAddress>();
    }

    //---------------------------------------------------------------------------------------------
    public static BlacklistMgr getInstance()
    {
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------
    public void init( final FloodlightModuleContext context )
    {
        LOG.debug( "Read configured blacklist' records." );

        LOG.debug( "Read DNS blacklist. File {}", DNSBlacklistFilename );
        mDnsBlacklist = readDnsBlacklistFile();
        dumpDnsBlacklist();

        LOG.debug( "Read IPv4 blacklist. File {}", IPv4BlacklistFilename );
        mIpv4Blacklist = readIpv4BlacklistFile();
        dumpIpv4Blacklist();
    }

    //---------------------------------------------------------------------------------------------
    public boolean checkIpv4Blacklist( InetAddress ipAddr ) { return mIpv4Blacklist.contains(ipAddr); }
    public void    addDnsRecord(    String record ) { mDnsBlacklist.add( record.toLowerCase() ); }
    public void    removeDnsRecord( String record ) { mDnsBlacklist.remove( record.toLowerCase() ); }

    //---------------------------------------------------------------------------------------------
    public boolean checkDnsBlacklist( String domainName )
    {
        String domainNameLC= domainName.toLowerCase();

        if( mDnsBlacklist. contains( domainNameLC ) ) return true; // If string matches, return true.

       if( !domainNameLC.startsWith( "www." ) ) {
            if( mDnsBlacklist.contains( "www." + domainNameLC ) ) return true; // Add "www.", if result matches, return true.
        }

        return false;  // No match, return false.
    }

    //---------------------------------------------------------------------------------------------
    public void addIpv4Record( InetAddress record )
    {
        mIpv4Blacklist.add( record );

        // Delete flow with target destination IP address on all connected switches
        FlowMgr.getInstance().deleteIPFlowOnAllConnectedSwitches( record );
    }

    //---------------------------------------------------------------------------------------------
    public void removeIpv4Record( InetAddress record ) { mIpv4Blacklist.remove( record ); }

    //---------------------------------------------------------------------------------------------
    public void saveDnsBlacklist()
    {
        try
        {
            File        file = new File( DNSBlacklistFilename );
            PrintWriter out  = new PrintWriter( new FileWriter(file) );

            for( String str : mDnsBlacklist ) { out.println(str); }
            out.close();
        }
        catch( Exception e )
        {
            LOG.error( "Unable to save modified DNS blacklist in a file {}. {}", DNSBlacklistFilename, e );
        }
    }

    //---------------------------------------------------------------------------------------------
    public void saveIpv4Blacklist()
    {
        try
        {
            File file = new File( IPv4BlacklistFilename );
            PrintWriter out = new PrintWriter( new FileWriter( file ) );

            for( InetAddress ipAddr : mIpv4Blacklist ) { out.println(ipAddr.getHostAddress()); }
            out.close();
        }
        catch (Exception e)
        {
            LOG.error("Unable to save modified IPv4 blacklist in a file {}. {}", IPv4BlacklistFilename, e);
        }
    }

    //---------------------------------------------------------------------------------------------
    public HashSet<String> getDnsBlacklistConfig() { return mDnsBlacklist; }

    //---------------------------------------------------------------------------------------------
    public HashSet<String> getIpv4BlacklistConfig()
    {
        HashSet<String> ipv4Str = new HashSet<String>();

        for( InetAddress ipAddr : mIpv4Blacklist ) { ipv4Str.add(ipAddr.getHostAddress()); }
        return ipv4Str;
    }

    //---------------------------------------------------------------------------------------------
    private void dumpDnsBlacklist()
    {
        String outString = "DNS blacklist:\n";

        Iterator<String> it = mDnsBlacklist.iterator();
        while( it.hasNext() ) { outString += it.next().toString() + "\n"; }

        LOG.debug(outString);
    }

    //---------------------------------------------------------------------------------------------
    private void dumpIpv4Blacklist()
    {
        String outString = "Ipv4 blacklist:\n";

        Iterator<InetAddress> it = mIpv4Blacklist.iterator();
        while( it.hasNext() ) { outString += it.next().getHostAddress() + "\n"; }

        LOG.debug(outString);
    }

    //---------------------------------------------------------------------------------------------
    private HashSet<InetAddress> readIpv4BlacklistFile()
    {
        HashSet<InetAddress> ipSet = new HashSet<InetAddress>();
        File file = new File(IPv4BlacklistFilename);
        try
        {
            FileInputStream f = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(f));
            String line;
            while ((line = br.readLine()) != null)
            {
                InetAddress ipAddr = convertIpv4Address(line); // Convert IP address to InetAddress

                if( ipAddr == null ) LOG.error( "Unable to parse IPv4 address \"{}\"", line );
                else ipSet.add( ipAddr );
            }
        }
        catch (FileNotFoundException e)
        {
            LOG.error("File \"{}\" not found.", IPv4BlacklistFilename);
            e.printStackTrace();
        }
        catch (IOException e)
        {
            LOG.error("Unable to read file \"{}\". {}", IPv4BlacklistFilename, e);
            e.printStackTrace();
        }

        return ipSet;
    }

    //---------------------------------------------------------------------------------------------
    public static InetAddress convertIpv4Address(String line)
    {

        if( line == null || line.isEmpty() )                return null;  // return if line is empty or null
        if( (line.length() < 7 ) & ( line.length() > 15 ) ) return null;  // return if line is too short or long

        try
        {   // Examine the input IP address and make sure it is valid
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches())
            {
                try { return InetAddress.getByName( line ); }  // Return the IP address InetAddress object
                catch (UnknownHostException e) {}
            }
        }
        catch (PatternSyntaxException ex) {}

        return null;  // If we were ultimately unsuccessful in parsing or translating the IP address, return null.
    }

    //---------------------------------------------------------------------------------------------
    private HashSet<String> readDnsBlacklistFile()
    {
        HashSet<String> fileLines = new HashSet<String>();
        File file = new File(DNSBlacklistFilename);
        try
        {
            FileInputStream f  = new FileInputStream( file );
            BufferedReader  br = new BufferedReader( new InputStreamReader( f ) );

            String line;
            while( ( line = br.readLine() ) != null )
            {
                fileLines.add( line.toLowerCase() );
            }
        }
        catch (FileNotFoundException e)
        {
            LOG.error("File \"{}\" not found.", DNSBlacklistFilename);
            e.printStackTrace();
        }
        catch (IOException e)
        {
            LOG.error("Unable to read file \"{}\". {}", DNSBlacklistFilename, e);
            e.printStackTrace();
        }

        return fileLines;
    }

}
