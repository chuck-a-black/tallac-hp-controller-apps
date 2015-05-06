/*
   Copyright 2012 IBM

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

// Create our namespace
window.Blacklist = window.Blacklist || {};

// Models
Blacklist.TallacDnsConfigRecord = Backbone.Model.extend( {} );
Blacklist.TallacIpConfigRecord  = Backbone.Model.extend( {} );

// Colections
Blacklist.TallacDnsBlacklist = Backbone.Collection.extend({
   
  model: Blacklist.TallacDnsConfigRecord,
  
  url: '/tallac/api/blacklist/sites/dns'

});

Blacklist.TallacIpBlacklist = Backbone.Collection.extend( {

  model: Blacklist.TallacIpConfigRecord,

  url: '/tallac/api/blacklist/sites/ip'

});

Blacklist.TallacBlacklistRecord = Backbone.Model.extend( {

} );

Blacklist.TallacBlacklistDetails = Backbone.Collection.extend({

    model: Blacklist.TallacBlacklistRecord,

    url: '/tallac/api/blacklist/stats/details'
});

Blacklist.TallacBlacklistStats = Backbone.Model.extend({

    url: '/tallac/api/blacklist/stats',
    
    defaults: {
      id : undefined,
      ipv4LastMatch: '',
      ipv4Count: 0,
      ipv4LastMatchTimestamp: '',
      dnsLastMatch: '',
      dnsCount: 0,
      dnsLastMatchTimestamp: '',
    }
});