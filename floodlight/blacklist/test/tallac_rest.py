#
# Copyright (c) 2012, Elbrys Networks
# All Rights Reserved.
#
# Note: This needs python requests library. On ubuntu run:
#       sudo apt-get install python-requests to install
#

import base64
import hashlib
import hmac
import json
import requests
import sys
import urllib
import urlparse
import logging

BASE_URL = "http://localhost:8080/tallac"

class RestException(Exception):
    def __init__(self, msg, url=None, status_code=None, contents=None):
        self.arg = (msg, url, status_code, contents)
        self.msg = msg
        self.url = url
        self.status_code = status_code
        self.contents = contents

    def __str__(self):
        s = "Rest Exception: %s" % (self.msg)
        if self.url:
            s += "\nurl: %s" % (self.url)
        if self.status_code:
            s += "\nstatus code: %s" % (self.status_code)
        if self.contents:
            s += "\ncontents: %s" % (self.contents)
        return s

def get_request(url):
    http_headers = {'Accept' : 'application/json'}

    try:
        url = BASE_URL + url
        print "url == " + url
        resp = requests.get(url, headers=http_headers)
        if resp.status_code != 200:
            raise RestException("Rest Request Failed", url, resp.status_code,
                              resp.content)

    except requests.ConnectionError:
        raise RestException("Connection error")

    print resp.content
    return json.loads(resp.content)

def post_request(url, body):
    http_headers = {'Content-type' : 'application/json'}

    print "POST url = %s" % (BASE_URL + url)
    print "POST http_headers = %s" % (http_headers)
    print "POST body = %s" % (body)

    try:
        url = BASE_URL + url
        resp = requests.post(url, headers=http_headers, data=body)
        if resp.status_code != 200:
            raise RestException("Rest Request Failed", url, resp.status_code,
                              resp.content)

    except requests.ConnectionError:
        raise RestException("Connection error")

    return resp.content

def delete_request(url, body):
    http_headers = {'Content-type' : 'application/json'}

    print "DELETE url = %s" % (BASE_URL + url)
    print "DELETE http_headers = %s" % (http_headers)
    print "DELETE body = %s" % (body)

    try:
        url = BASE_URL + url
        resp = requests.delete(url, headers=http_headers, data=body)
        if resp.status_code != 200:
            raise RestException("Rest Request Failed", url, resp.status_code,
                              resp.content)

    except requests.ConnectionError:
        raise RestException("Connection error")

    return resp.content

def get_stats():
    url = "/api/blacklist/stats"
    return get_request(url)
    
def get_stats_details():
    url = "/api/blacklist/stats/details"
    return get_request(url)
    
def get_dns_config():
    url = "/api/blacklist/sites/dns"
    return get_request(url)

def add_dns_config(record):
    url = "/api/blacklist/sites/dns"
    json_dict = {};
    json_dict['record'] = record
    body = json.dumps(json_dict)
    return post_request(url, body)

def delete_dns_config(record):
    url = "/api/blacklist/sites/dns"
    json_dict = {};
    json_dict['record'] = record
    body = json.dumps(json_dict)
    return delete_request(url, body)
    
def get_ipv4_config():
    url = "/api/blacklist/sites/ipv4"
    return get_request(url)

def add_ipv4_config(record):
    url = "/api/blacklist/sites/ipv4"
    json_dict = {};
    json_dict['record'] = record
    body = json.dumps(json_dict)
    return post_request(url, body)

def delete_ipv4_config(record):
    url = "/api/blacklist/sites/ipv4"
    json_dict = {};
    json_dict['record'] = record
    body = json.dumps(json_dict)
    return delete_request(url, body)


