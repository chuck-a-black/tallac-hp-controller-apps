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

def put_request(url, body):
    http_headers = {'Content-type' : 'application/json'}

    print "PUT url = %s" % (BASE_URL + url)
    print "PUT http_headers = %s" % (http_headers)
    print "PUT body = %s" % (body)

    try:
        url = BASE_URL + url
        resp = requests.put(url, headers=http_headers, data=body)
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

def get_users(mac, ip):
    url = "/api/nac/users"
    if mac != None :
      url = url + "?mac=" + mac
    elif ip != None :
      url = url + "?ip=" + ip
    return get_request(url)
    
def change_user(mac, state, details):
    url = "/api/nac/users"
    json_dict = {};
    json_dict['mac'] = mac
    json_dict['ip'] = 0
    if state != None:
       json_dict['state'] = state
    if details != None:
       json_dict['details'] = details
    body = json.dumps(json_dict)
    return put_request(url, body)
    
def get_logs():
    url = "/api/nac/logs"
    return get_request(url)
    
def auth_guest(ip):
    url = "/api/nac/users/auth"
    json_dict = {};
    json_dict['authtype'] = 'guest'
    if ip != None:
       json_dict['ip'] = ip
    body = json.dumps(json_dict)
    return put_request(url, body)
    
def auth_client(ip, username, password):
    url = "/api/nac/users/auth"
    json_dict = {};
    json_dict['authtype'] = 'client'
    if ip != None:
       json_dict['ip'] = ip
    json_dict['username'] = username
    json_dict['password'] = password
    
    body = json.dumps(json_dict)
    return put_request(url, body)
