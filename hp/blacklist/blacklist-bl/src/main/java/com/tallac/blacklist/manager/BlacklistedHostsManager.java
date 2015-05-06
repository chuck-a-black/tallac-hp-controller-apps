package com.tallac.blacklist.manager;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.listener.PacketListener;

//import com.tallac.blacklist.module.FlowMgr;

public class BlacklistedHostsManager {
	
	private static final String HOME = "/home/chuck/dev/sdn-apps/blacklist/";

    private static final BlacklistedHostsManager INSTANCE = new BlacklistedHostsManager();
    
    private static final Logger LOG = LoggerFactory.getLogger( BlacklistedHostsManager.class );
    private static final String LOGPREFACE = "[Blacklist: BlacklistedHostsManager]: ";        

    private static final String DNSBlacklistFilename  = HOME + "dnsBlacklist.txt";
    private static final String IPv4BlacklistFilename = HOME + "ipv4Blacklist.txt";

    //---- Two hashes, one for DNS and one for IP traffic
    HashSet<String>      mDnsBlacklist;
    HashSet<InetAddress> mIpv4Blacklist;

    //---------------------------------------------------------------------------------------------
    private BlacklistedHostsManager()
    {
        // private constructor - prevent external instantiation
        mDnsBlacklist  = new HashSet<String>();
        mIpv4Blacklist = new HashSet<InetAddress>();
    }

    //---------------------------------------------------------------------------------------------
    public static BlacklistedHostsManager getInstance()
    {
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------
    public void init()
    {
        LOG.info( LOGPREFACE + "init(): Read configured blacklist' records." );

        LOG.info( LOGPREFACE + "init(): Read DNS blacklist. File {}", DNSBlacklistFilename );
        mDnsBlacklist = readDnsBlacklistFile();
        dumpDnsBlacklist();

        LOG.info( LOGPREFACE + "init(): Read IPv4 blacklist. File {}", IPv4BlacklistFilename );
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
        //TODOHP: FlowMgr.getInstance().deleteIPFlowOnAllConnectedSwitches( record );
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
        String outString = "DNS blacklist: [";

        Iterator<String> it = mDnsBlacklist.iterator();
        while( it.hasNext() ) { outString += it.next().toString() + ","; }

        LOG.info( LOGPREFACE + "dumpDnsBlacklist(): " + outString + "]" );
    }

    //---------------------------------------------------------------------------------------------
    private void dumpIpv4Blacklist()
    {
        String outString = "Ipv4 blacklist: [";

        Iterator<InetAddress> it = mIpv4Blacklist.iterator();
        while( it.hasNext() ) { outString += it.next().getHostAddress() + ","; }

        LOG.info( LOGPREFACE + "dumpIpBlacklist(): " + outString + "]" );
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
            LOG.info( LOGPREFACE + "readDnsBlacklistFile(): File \"{}\" not found.", DNSBlacklistFilename );
            e.printStackTrace();
        }
        catch (IOException e)
        {
            LOG.info( LOGPREFACE + "readDnsBlacklistFile(): Unable to read file \"{}\". {}", DNSBlacklistFilename, e );
            e.printStackTrace();
        }

        return fileLines;
    }


}
