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
'use strict';    
window.Nac = window.Nac || {};
Nac.AppView = Backbone.View.extend({

    template: _.template( $( '#tallacnac' ).html() ),

    initialize: function () {
        this.nacClientView = null;
        this.nacLogView = null;
    },

    render: function ( eventName ) {
        $( this.el ).html( this.template() );

        this.clientModel  = new Nac.Devices();
		this.nacClientView = new Nac.ClientsView( { model: this.clientModel } );
        this.$( '#tallac-nac-client-list' ).html( this.nacClientView.render().el );
        
        this.logModel  = new Nac.Logs();
		this.nacLogView = new Nac.LogsView( { model: this.logModel } );
        this.$( '#tallac-nac-log-list' ).html( this.nacLogView.render().el );
        return this;
    }
});

Nac.ClientsView = Backbone.View.extend({

    template: _.template( $( '#tallac-nac-client-list' ).html() ),

    initialize: function () {
        this.listenTo( this.model, 'add', this.addOne );
        this.model.fetch();

        if( Nac.clientIntervalId ) { clearInterval( Nac.clientIntervalId ); }
        Nac.clientIntervalId = setInterval( this.updateClients, 5000, this.model );
    },

    updateClients: function( model ) {
        model.fetch();
    },

    render: function ( eventName ) {
       $( this.el) .html( this.template() );
        return this;
    },

    addOne: function( client ) {
        this.$( '#client-list' ).prepend( new Nac.ClientItemView( { model:client } ).render().el );
    }
});

Nac.ClientItemView = Backbone.View.extend({

    template: _.template( $( '#tallac-nac-client-item' ).html() ),

    tagName: 'li',

    events: {
        'click #resetClient': 'deleteFlows',
    },

    initialize: function () {
        this.listenTo( this.model, 'sync', this.render );
        this.listenTo( this.model, 'change', this.render );
        this.listenTo( this.model, 'destroy', this.remove );
    },

    render: function ( eventName ) {
        $( this.el ).html( this.template( this.model.toJSON() ) );
        return this;
    },
    
    deleteFlows: function(e) {
        e.preventDefault();
        this.model.save( { state: 'UNAUTHENTICATED' } );
    },
});

Nac.LogsView = Backbone.View.extend({

    template: _.template( $( '#tallac-nac-log-list' ).html() ),

    initialize: function () {
        this.listenTo( this.model, 'add', this.addOne );
        this.model.fetch();
        if( Nac.logIntervalId ) { clearInterval( Nac.logIntervalId ); }
        Nac.logIntervalId = setInterval( this.updateLogs, 5000, this.model );
    },

    updateLogs: function( model ) {
        model.fetch();
    },

    render:function (eventName) {
       $( this.el ).html( this.template() );
        return this;
    },

    addOne: function ( log ) {
        this.$( '#log-list' ).prepend( new Nac.LogItemView( { model: log } ).render().el );        
    }
});

Nac.LogItemView = Backbone.View.extend({

    template: _.template( $( '#tallac-nac-log-item' ).html() ),

    tagName: "li",

    render:function (eventName) {
        var log = this.model.toJSON();
        log.formattedTime = moment( log.time ).format( 'MMMM Do, h:mm:ss a' );
        $( this.el ).html( this.template( log ) );
        return this;
    }
});
