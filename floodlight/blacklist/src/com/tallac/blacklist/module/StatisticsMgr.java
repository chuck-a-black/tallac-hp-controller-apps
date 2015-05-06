/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.module;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.packet.IPv4;

import org.codehaus.jackson.annotate.JsonProperty;
import org.openflow.protocol.OFMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.utils.CircularList;

public class StatisticsMgr
{
    private static final StatisticsMgr INSTANCE = new StatisticsMgr();
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsMgr.class);
    private static final long   LOG_STATISTICS_TIMEOUT = 60;

    //---------------------------------------------------------------------------------------------
    public class StatisticsTotal
    {
        private final AtomicLong m_matchCounter;
        private Date             m_lastMatchTime;
        private String           m_lastMatch;

        //---------------------------------------------------------------------------------------------
       public StatisticsTotal()
        {
            m_matchCounter = new AtomicLong(0);
            m_lastMatchTime = new Date();
            m_lastMatch = "";
        }

       //---------------------------------------------------------------------------------------------
        public void setMatch(String match, Date date)
        {
            m_matchCounter.incrementAndGet();
            m_lastMatchTime = date;
            m_lastMatch = match;
        }

        //---------------------------------------------------------------------------------------------
        public String getMatch()       { return m_lastMatch; }
        public Date getLastMatchTime() { return m_lastMatchTime; }
        public long getMatchCounter()  { return m_matchCounter.get(); }
    }

    //---------------------------------------------------------------------------------------------
    public class StatisticsDetails
    {
        @JsonProperty("id")
        long mId;

        @JsonProperty("entry")
        String mBlockedEntry;

        @JsonProperty("time")
        long mTime;

        @JsonProperty("switchId")
        String mSwitchDpid;

        @JsonProperty("inputPort")
        int mInputPort;

        @JsonProperty("sourceIp")
        long mSourceIp;

        //---------------------------------------------------------------------------------------------
        public StatisticsDetails() {}
        public StatisticsDetails( final IOFSwitch ofSwitch,
                                  final OFMatch   match,
                                  String          entryDescr,
                                  Date            date )
        {
            mBlockedEntry = entryDescr;
            mTime         = date.getTime();
            mId           = getTotalMatchCounter();
            mSwitchDpid   = ofSwitch.getStringId();
            mInputPort    = match.getInputPort();
            mSourceIp     = match.getNetworkSource();
        }

        public String getSourceIp()
        {
            String ipAddrStr;

            try { ipAddrStr = InetAddress.getByAddress( IPv4.toIPv4AddressBytes( (int)mSourceIp)).getHostAddress(); }
            catch (UnknownHostException e) {
                ipAddrStr = "N/A";  // Unable to get ipv4 address string representation
            }
            return ipAddrStr;
        }

    }

    private final StatisticsTotal mIpv4Stats;
    private final StatisticsTotal mDnsStats;
    private final CircularList<StatisticsDetails> mEntryDetails;
    private final static int MAX_NUMBER_OF_DETAILED_ENTRIES = 100;
    private final ScheduledExecutorService mScheduler;

    //---------------------------------------------------------------------------------------------
    private StatisticsMgr()
    {
        // private constructor - prevent external instantiation
        mScheduler    = Executors.newScheduledThreadPool(2);
        mIpv4Stats    = new StatisticsTotal();
        mDnsStats     = new StatisticsTotal();
        mEntryDetails = new CircularList<StatisticsDetails>(MAX_NUMBER_OF_DETAILED_ENTRIES);
    }

    //---------------------------------------------------------------------------------------------
    public static StatisticsMgr getInstance() { return INSTANCE; }

    //---------------------------------------------------------------------------------------------
    public void init( final FloodlightModuleContext context )
    {
        mScheduler.scheduleAtFixedRate(new Runnable()
        {
            public void run()
            {
                StringBuffer statsMsg = new StringBuffer();
                statsMsg.append("IPv4 Statistics:");
                if (mIpv4Stats.getMatchCounter() > 0)
                {
                    statsMsg.append(" Total matches: ").append(mIpv4Stats.getMatchCounter()).append(". Last match: ")
                            .append(mIpv4Stats.getMatch()).append(" ").append(mIpv4Stats.getLastMatchTime()).append(".\n");
                }
                else
                {
                    statsMsg.append(" No IPv4 matches.\n");
                }

                statsMsg.append("  DNS Statistics:");
                if (mDnsStats.getMatchCounter() > 0)
                {
                    statsMsg.append(" Total matches: ").append(mDnsStats.getMatchCounter()).append(". Last match: ")
                    .append(mDnsStats.getMatch()).append(" ").append(mDnsStats.getLastMatchTime()).append(".");
                }
                else
                {
                    statsMsg.append(" No DNS matches.");
                }

                LOG.info(statsMsg.toString());
            }
        }, 0, LOG_STATISTICS_TIMEOUT, TimeUnit.SECONDS);

    }

    public StatisticsTotal getIpv4Stats()        { return mIpv4Stats; }
    public StatisticsTotal getDnsStats()         { return mDnsStats; }
    public long getTotalMatchCounter()           { return mIpv4Stats.getMatchCounter() + mDnsStats.getMatchCounter(); }

    public List<StatisticsDetails> getDetails()  { return mEntryDetails.get(); }

    //---------------------------------------------------------------------------------------------
    public void updateIpv4Stats( final IOFSwitch ofSwitch, final OFMatch match, InetAddress ipAddr )
    {
        Date ts = new Date();
        String ipv4DestStr =  ipAddr.getHostAddress();
        mIpv4Stats.setMatch( ipv4DestStr, ts );  // Modify total statistic

        // Add records to the list of detailed statistics
        StatisticsDetails statDetails = new StatisticsDetails(ofSwitch, match, ipv4DestStr, ts);
        mEntryDetails.add(statDetails);
    }

    //---------------------------------------------------------------------------------------------
    public void updateDnsStats( final IOFSwitch ofSwitch, final OFMatch match, String domainName )
    {
        Date ts = new Date();
        mDnsStats.setMatch( domainName, ts) ;    // Modify total statistic

        // Add records to the list of detailed statistics
        StatisticsDetails statDetails = new StatisticsDetails(ofSwitch, match, domainName, ts);
        mEntryDetails.add(statDetails);
    }
}
