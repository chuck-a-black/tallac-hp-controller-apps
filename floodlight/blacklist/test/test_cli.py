#!/usr/bin/python
#
# Copyright (c) 2012, Elbrys Networks
# All Rights Reserved.
#

import tallac_rest
import argparse
import logging
import re
import sys
import exceptions

PROG_NAME="test_cli"

def command_get_stats():
    return tallac_rest.get_stats()

def command_get_stats_details():
    return tallac_rest.get_stats_details()

def command_get_dns_config():
    return tallac_rest.get_dns_config()

def command_add_dns_config(record):
    return tallac_rest.add_dns_config(record)

def command_delete_dns_config(record):
    return tallac_rest.delete_dns_config(record)

def command_get_ipv4_config():
    return tallac_rest.get_ipv4_config()

def command_add_ipv4_config(record):
    return tallac_rest.add_ipv4_config(record)

def command_delete_ipv4_config(record):
    return tallac_rest.delete_ipv4_config(record)

def parse_args():
    parser = argparse.ArgumentParser(PROG_NAME)

    subparser = parser.add_subparsers(help='commands', dest="sub_command")

    # 'getStats' sub-command
    get_stats_parser = subparser.add_parser('getStats',
                               help='Get Blacklist statistics')
    # 'getStatsDetails' sub-command
    get_stats_details_parser = subparser.add_parser('getStatsDetails',
                               help='Get Blacklist statistics details')
    # 'getDnsConfig' sub-command
    get_dns_config_parser = subparser.add_parser('getDnsConfig',
                               help='Get DNS Blacklists configuration')
    # 'addDnsRecord' sub-command
    add_dns_config_parser = subparser.add_parser('addDnsRecord',
                               help='Add domain name to DNS Blacklists configuration')
    add_dns_config_parser.add_argument('record', action='store',
                               help='domain name')
    # 'deleteDnsRecord' sub-command
    delete_dns_config_parser = subparser.add_parser('deleteDnsRecord',
                               help='Delete domain name from DNS Blacklists configuration')
    delete_dns_config_parser.add_argument('record', action='store',
                               help='domain name')
    # 'getIpv4Config' sub-command
    get_ipv4_config_parser = subparser.add_parser('getIpv4Config',
                               help='Get DNS Blacklists configuration')
    # 'addIpv4Record' sub-command
    add_ipv4_config_parser = subparser.add_parser('addIpv4Record',
                               help='Add IPv4 address to IPv4 Blacklists configuration')
    add_ipv4_config_parser.add_argument('record', action='store',
                               help='IPv4 address')
    # 'deleteIpv4Record' sub-command
    delete_ipv4_config_parser = subparser.add_parser('deleteIpv4Record',
                               help='Delete IPv4 address from IPv4 Blacklists configuration')
    delete_ipv4_config_parser.add_argument('record', action='store',
                               help='IPv4 address')

    return parser.parse_args()

def main():
    args = parse_args()

    print "Specified sub-command %s" % (args.sub_command)

    if args.sub_command == "getStats":
        return command_get_stats()

    if args.sub_command == "getStatsDetails":
        return command_get_stats_details()

    elif args.sub_command == "getDnsConfig":
        return command_get_dns_config()

    elif args.sub_command == "addDnsRecord":
        return command_add_dns_config(args.record)

    elif args.sub_command == "deleteDnsRecord":
        return command_delete_dns_config(args.record)

    elif args.sub_command == "getIpv4Config":
        return command_get_ipv4_config()

    elif args.sub_command == "addIpv4Record":
        return command_add_ipv4_config(args.record)

    elif args.sub_command == "deleteIpv4Record":
        return command_delete_ipv4_config(args.record)

    else: # should never get here
        print "Unknown subcommand specified '%s'" % (args.sub_command)
        sys.exit(1)

if __name__ == "__main__":
    FORMAT = '%(asctime)-15s %(levelname)-8s: %(message)s'
    logging.basicConfig(#level=logging.INFO,
                        level=logging.DEBUG,
                        format=FORMAT)
    main()
