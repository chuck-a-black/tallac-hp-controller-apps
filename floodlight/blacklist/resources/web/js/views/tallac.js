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

Blacklist.TallacDnsBlacklistItemView = Backbone.View.extend( {

    tagName: 'li',

    events: {
        'click .destroy': 'clear'
    },

    template: _.template( $( '#dns-template' ).html() ),

    initialize:function () {
        this.listenTo( this.model, 'change', this.render );
        this.listenTo( this.model, 'destroy', this.remove );
    },

    render:function ( eventName ) {
        $( this.el ).html( this.template( this.model.toJSON() ) );
        return this;
    },
    
    clear: function () {
        this.model.destroy();
    }
});

Blacklist.TallacIpBlacklistItemView = Backbone.View.extend({

    tagName: "li",

    events: {
        'click .destroy': 'clear'
    },

    template: _.template( $( '#ip-template' ).html() ),

    initialize:function () {
        this.listenTo( this.model, 'change', this.render );
        this.listenTo( this.model, 'destroy', this.remove );
    },

    render:function ( eventName ) {
        $( this.el ).html( this.template( this.model.toJSON() ) );
        return this;
    },
     
    clear: function () {
        this.model.destroy();
    }
});

Blacklist.TallacDnsBlacklistView = Backbone.View.extend({

    events: {
        'keypress #new-dns': 'createOnEnter',
    },

    template: _.template( $( '#dnsapp-template' ).html() ),
    
    $input: function() {
        return this.$( '#new-dns' );
    },

    initialize: function () {
        this.listenTo( this.model, 'add', this.addOne );
        this.model.fetch();
    },

    render: function (eventName) {
       $( this.el ).html( this.template() );
       this.model.each( this.addOne, this );
        return this;
    },

    addOne: function( dns ) {
        var view = new Blacklist.TallacDnsBlacklistItemView( { model: dns } );
        this.$( '#dns-list' ).prepend( view.render().el );
    },

    createOnEnter: function ( e ) {
        if ( e.which !== 13 || !this.$input().val().trim() ) {
            return;
        }
        this.model.create( this.newAttributes() );
        this.$input().val( '' );
    },

    newAttributes: function() {
        return {
            id: undefined,
            record: this.$input().val()
        }
    }
});

Blacklist.TallacIpBlacklistView = Backbone.View.extend({

    events: {
        'keypress #new-ip' : 'createOnEnter'
    },

    template: _.template( $( '#ipapp-template' ).html() ),

    $input: function() {
        return this.$( '#new-ip' );
    },

    initialize: function () {
        this.listenTo( this.model, 'add', this.addOne );
        this.model.fetch();
    },

    render:function (eventName) {
       $( this.el ).html( this.template() );
        this.model.each( this.addOne, this );
       return this;
    },
    
    addOne: function( ip ) {
        var view = new Blacklist.TallacIpBlacklistItemView( { model: ip } );
        this.$('#ip-list').prepend( view.render().el );
    },

    newAttributes: function() {
        return {
            id: undefined,
            record: this.$input().val()
        }
    },

    createOnEnter: function ( e ) {
        if ( e.which !== 13 || !this.$input().val().trim() ) {
            return;
        }
        this.model.create( this.newAttributes() );
        this.$input().val( '' );
    }
});

Blacklist.TallacBlacklistDetailsView = Backbone.View.extend({

    template: _.template( $( '#stats-template' ).html() ),

    initialize: function () {
        this.listenTo( this.model, 'add', this.addOne );
        this.model.fetch();
        if( Blacklist.intervalId ) { clearInterval( Blacklist.intervalId ); }
        Blacklist.intervalId = setInterval( this.updateStats, 5000, this.model );
    },

    updateStats: function ( model ) {
        model.fetch();
    },

    render: function ( eventName ) {
       $( this.el ).html( this.template() );
        this.model.each( this.addOne, this );
        return this;
    },

    addOne: function( stat ) {
        var view = new Blacklist.TallacBlacklistDetailsItemView( { model: stat } );
        this.$('#log-list').prepend( view.render().el );
    },
});

Blacklist.TallacBlacklistDetailsItemView = Backbone.View.extend({

    tagName:"li",

    template: _.template( $( '#stat-item-template' ).html() ),

    render:function (eventName) {
        var stat = this.model.toJSON();
        stat.formattedTime = moment( stat.time ).format( 'MMMM Do, h:mm:ss a' );
        $( this.el ).html( this.template( stat ) );
        return this;
    }
});

Blacklist.TallacBlacklistStatsView = Backbone.View.extend( {

    template: _.template( $(' #stats-summary-template' ).html() ),

    initialize: function () {
        this.listenTo( this.model, 'change', this.render );
        this.model.fetch();
        if( Blacklist.intervalIdStat ) { clearInterval( Blacklist.intervalIdStat ); }
        Blacklist.intervalIdStat = setInterval( this.updateStats, 5000, this.model );
    },

    updateStats: function ( model ) {
        model.fetch();
    },

    render: function ( eventName ) {
        var stat = this.model.toJSON();
        stat.formattedTimeIp = stat.ipv4LastMatchTimestamp ? moment( stat.ipv4LastMatchTimestamp ).format( 'MMMM Do, h:mm:ss a' ) : " ";
        stat.formattedTimeDns = stat.dnsLastMatchTimestamp ? moment( stat.dnsLastMatchTimestamp ).format( 'MMMM Do, h:mm:ss a' ) : " ";
        $( this.el ).html( this.template( stat ) );
        return this;
    }
});

Blacklist.TallacView = Backbone.View.extend({

    template: _.template( $( '#tallac-template' ).html() ),

    initialize: function() {
        this.dnsEntries   = new Blacklist.TallacDnsBlacklist();        
        this.ipEntries    = new Blacklist.TallacIpBlacklist();
        this.stats        = new Blacklist.TallacBlacklistDetails();
        this.statsSummary = new Blacklist.TallacBlacklistStats();

        this.dnsEntries.comparator = 'record';
        this.ipEntries.comparator  = 'record';
        this.stats.comparator      = 'time';


        this.tdblView         = new Blacklist.TallacDnsBlacklistView    ( { model: this.dnsEntries } );
        this.tiblView         = new Blacklist.TallacIpBlacklistView     ( { model: this.ipEntries } );
        this.tblsdView        = new Blacklist.TallacBlacklistDetailsView( { model: this.stats } );
        this.statsSummaryView = new Blacklist.TallacBlacklistStatsView  ( { model: this.statsSummary } );
    },

    render: function ( eventName ) {
        $( this.el ).html( this.template() );

        $( this.el ).find( '#dns-list-placeholder' ).html( this.tdblView.render().el );
        $( this.el ).find( '#ip-list-placeholder' ).html( this.tiblView.render().el );
        $( this.el ).find( '#tallac-blacklist-details-list' ).html( this.tblsdView.render().el );
        $( this.el ).find( '#tallac-blacklist-stats' ).html( this.statsSummaryView.render().el );

        return this;
    }
});