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
import requests
import exceptions

PROG_NAME="test_cli"

def validate_mac_address(mac):
    if not re.match("[0-9a-f]{2}([-:][0-9a-f]{2}){5}$", mac.lower()):
        print "Invalid MAC address specified"
        sys.exit(1)

def command_get_users(mac, ip):
    return tallac_rest.get_users(mac, ip)
    
def command_change_user(mac, state, details):
    return tallac_rest.change_user(mac, state, details)

def command_get_logs():
    return tallac_rest.get_logs()
    
def command_auth_guest(ip):
    return tallac_rest.auth_guest(ip)
    
def command_auth_client(ip, username, password):
    return tallac_rest.auth_client(ip, username, password)

def parse_args():
    parser = argparse.ArgumentParser(PROG_NAME)

    subparser = parser.add_subparsers(help='commands', dest="sub_command")

    # 'getUsers' sub-command
    get_users_parser = subparser.add_parser('getUsers',
                               help='Get NAC users')
    get_users_parser.add_argument('--mac', action='store', default=None,
                               help='Get users with specified MAC Address')
    get_users_parser.add_argument('--ip', action='store', default=None,
                               help='Get users with specified IPv4 Address')
                               
    # 'changeUsers' sub-command
    change_users_parser = subparser.add_parser('changeUser',
                               help='Modify user data')
    change_users_parser.add_argument('mac', action='store',
                               help='User\'s MAC Address')
    change_users_parser.add_argument('--state', action='store', nargs='?',
                               help='User state GUEST|AUTHENTICATED|UNAUTHENTICATED')
    change_users_parser.add_argument('--details', action='store', nargs='?',
                               help='User description')
                               
    # 'getLogs' sub-command
    get_logs_parser = subparser.add_parser('getLogs',
                               help='Get logs')
                               
     # 'authGuest' sub-command
    auth_guest_parser = subparser.add_parser('authGuest',
                               help='Authenticate as a guest')
    auth_guest_parser.add_argument('--ip', action='store', default=None, nargs='?',
                               help='Guest IP address. If not specified, IP address of REST request originator will be used as guest IP.')
                               
     # 'authClient' sub-command
    auth_client_parser = subparser.add_parser('authClient',
                               help='Authenticate as a client')
    auth_client_parser.add_argument('--ip', action='store', default=None, nargs='?',
                               help='Guest IP address. If not specified, IP address of REST request originator will be used as guest IP.')
    auth_client_parser.add_argument('--username', action='store',
                               help='User name')
    auth_client_parser.add_argument('--password', action='store',
                               help='Password')
                               
    return parser.parse_args()

def main():
    args = parse_args()

    print "Specified sub-command %s" % (args.sub_command)

    if args.sub_command == "getUsers":
        if args.mac != None :
           validate_mac_address(args.mac)
        if args.mac != None and args.ip != None :
           print "MAC and IP addresses cannot be specified at the same time."
           sys.exit(1)
           
        return command_get_users(args.mac, args.ip)
        
    if args.sub_command == "changeUser":
        if args.mac == None :
           print 'MAC argument is required'
           sys.exit(1)
        if args.mac != None :
           validate_mac_address(args.mac)
        if args.state == None and args.details == None:
           print "State or details should be specified."
           sys.exit(1)
        return command_change_user(args.mac, args.state, args.details)

    if args.sub_command == "getLogs":
        return command_get_logs()
        
    if args.sub_command == "authGuest":
        return command_auth_guest(args.ip)
        
    if args.sub_command == "authClient":
        if args.username == None or args.password == None :
           print "Username and password are mandatory parameters."
           sys.exit(1)
        return command_auth_client(args.ip, args.username, args.password)
        
    else: # should never get here
        print "Unknown subcommand specified '%s'" % (args.sub_command)
        sys.exit(1)

if __name__ == "__main__":
    FORMAT = '%(asctime)-15s %(levelname)-8s: %(message)s'
    logging.basicConfig(#level=logging.INFO,
                        level=logging.DEBUG,
                        format=FORMAT)
    main()
