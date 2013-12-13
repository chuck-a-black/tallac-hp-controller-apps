package com.tallac.blacklist.handler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.pkt.MessageContext;
import com.hp.of.lib.msg.OfmPacketIn;
import com.tallac.blacklist.listener.PacketListener;
import com.tallac.blacklist.manager.BlacklistedHostsManager;

public class DnsPacketHandler {

    public static final short   TYPE_IPv4  = 0x0800;
    public static final short   TYPE_8021Q = (short) 0x8100;
    
    private static final Logger LOG = LoggerFactory.getLogger( DnsPacketHandler.class );
    private static final String LOGPREFACE = "[Blacklist: DnsPacketHandler]: ";        

	//-------------------------------------------------------------------------
	public boolean allowDnsRequest(  MessageContext messageContext,
            					     OfmPacketIn    packetIn )
	{
		final byte[] packetInBytes = packetIn.getData();
        Collection<String> domainNames;

        LOG.trace( LOGPREFACE + "allowDnsRequest(): Beginning to parse DNS request." );
        
        // Extract the domain names from the DNS request.
        try {
            domainNames = parseDnsPacket( packetInBytes );
        }
        catch( IOException e )  // Got here if there was an exception in parsing the domain names in the DNS request.
        {
            LOG.info( LOGPREFACE + "allowDnsRequest(): Exception - unable to parse DNS query packet {}", packetInBytes );
            e.printStackTrace();

            return true;
        }

        LOG.trace( LOGPREFACE + "allowDnsRequest(): Extracted DNS names successfully: {}", domainNames );
        
        if( domainNames == null ) return true; // If there were no domain names, allow it.

        // Process all the domain names from the request, seeing if any cause us to drop the packet.
        for( String domainName : domainNames )
        {
        	LOG.trace( LOGPREFACE + "allowDnsRequest(): Checking domain name: {}", domainName );
        	
            //  If the current domainName is in the blacklist, take action immediately
            if( BlacklistedHostsManager.getInstance().checkDnsBlacklist( domainName ) )
            {
                LOG.info( LOGPREFACE + "allowDnsRequest(): DNS query packet dropped. Domain name: {}", domainName );

                return false;
            }
        }
        
        LOG.trace( LOGPREFACE + "allowDnsRequest(): All domain names okay, allow DNS to proceed." );
        
		return true;
	}
	
    //---------------------------------------------------------------------------------------------
    //  parseDnsPacket:  Called to parse the DNS request and extract and return the domain names
    //
    private Collection<String> parseDnsPacket( byte[] pkt ) throws IOException
    {

        final DataInputStream packetDataInputStream = new DataInputStream(new ByteArrayInputStream(pkt));
        int position = 0;

        // Skip Ethernet header: dst(6) and source(6) MAC, DataLayer type(2),
        packetDataInputStream.skip(6 + 6);
        short etherType = packetDataInputStream.readShort();
        position += 14;

        // Skip VLAN tags
        while( etherType == TYPE_8021Q )
        {
            @SuppressWarnings("unused")
            final short vlanId = packetDataInputStream.readShort();
            etherType = packetDataInputStream.readShort();
            position += 4;
        }

        if (etherType != TYPE_IPv4)
        {
            LOG.error("Unknown etherType. " + String.format("%04X ", etherType));
            return null;
        }

        // Parse IPv4 header
        final byte ipByte1 = packetDataInputStream.readByte();
        position += 1;
        final int version = (ipByte1 & 0xF0) >> 4;
        final int IHL = ipByte1 & 0x0F; // length in number of words

        //Check version
        if (version != 4)
        {
            LOG.error("Packet IP unknown version");
            return null;
        }

        //Check IP header length
        if (IHL < 5)
        {
            LOG.error("Packet IP header too small");
            return null;
        }

        // Skip IPv4 header
        packetDataInputStream.skip(IHL * 4 - 1);
        position += IHL * 4 - 1;

        // Parse UDP packet
        // Skip source port (2), destination port (2), length (2), checksum (2)
        // and query ID (2)
        packetDataInputStream.skip(2 + 2 + 2 + 2 + 2);
        position += 10;

        // read query flags, check QR bit (Query/Response)
        final short DNS_QR_BIT_NUMBER = (short) 15;
        if (((packetDataInputStream.readShort() >> DNS_QR_BIT_NUMBER) & 1) == 0)
        {
            position += 2;

            // Parse DNS query data, save domain names in String Collection
            Collection<String> domainNames = new ArrayList<String>();

            // Read number of queries
            int numQueries = packetDataInputStream.readShort();
            position += 2;

            // Skip numAnswers (2), numAuthorities (2), numAdditional (2);
            packetDataInputStream.skip(6);
            position += 6;

            // read queries
            for (int i = 0; i < numQueries; i++)
            {
                String dName =
                    DNSQueryQuestionParser.getDomainName(pkt, position);
                domainNames.add(dName);

                // Skip queryType (2), queryClass (2)
                packetDataInputStream.skip(4);
                position += 4;
            }

            if (domainNames.size() == 0)
            {
                LOG.error("Unable to parse domain question(s)");
                domainNames = null;
            }

            return domainNames;
        }

        return null;
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Class DNSQueryQuestionParser is responsible for parsing domain name
     * questions in DNS query packet.
     *
     * DNS query question parer code is based on class
     * org.apache.directory.server.dns.io.decoder.DnsMessageDecoder developed
     * for Apache DNS server project.
     *
     *  Licensed to the Apache Software Foundation (ASF) under one
     *  or more contributor license agreements.  See the NOTICE file
     *  distributed with this work for additional information
     *  regarding copyright ownership.  The ASF licenses this file
     *  to you under the Apache License, Version 2.0 (the
     *  "License"); you may not use this file except in compliance
     *  with the License.  You may obtain a copy of the License at
     *
     *    http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing,
     *  software distributed under the License is distributed on an
     *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     *  KIND, either express or implied.  See the License for the
     *  specific language governing permissions and limitations
     *  under the License.
     *
     */
    private static class DNSQueryQuestionParser
    {

        static String getDomainName(byte[] buf, int pos) throws IOException
        {
            StringBuffer domainName = new StringBuffer();
            recurseDomainName(buf, pos, domainName);

            return domainName.toString();
        }

        static void recurseDomainName( byte[] pkt, int pos, StringBuffer domainName ) throws IOException
        {
            final DataInputStream byteBuffer = new DataInputStream( new ByteArrayInputStream(pkt, pos, pkt.length - pos) );
            int length = byteBuffer.readUnsignedByte();

            if( ( length & 0xc0 ) == 0xc0 )
            {
                int position = byteBuffer.readUnsignedShort();
                int offset   = length & ~(0xc0) << 8;

                recurseDomainName( pkt, position + offset, domainName );
            }
            else if (length != 0 && (length & 0xc0) == 0)
            {
                int labelLength = length;

                /* read label */
                byte[] strBytes = new byte[labelLength];
                byteBuffer.readFully(strBytes);
                String label = new String(strBytes);
                domainName.append(label);
                if (byteBuffer.readByte() != 0)
                {
                    domainName.append(".");
                    recurseDomainName(pkt, pos + labelLength + 1, domainName);
                }
            }
        }

    }

}
