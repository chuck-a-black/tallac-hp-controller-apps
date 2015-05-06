'use strict';

$( function() { 
    window.Nac = window.Nac || {};

    Nac.Session = Backbone.Model.extend( {
        url: '/tallac/api/nac/users/auth',     

        validate: function( attrs, options ) {

            var errors = undefined;
            if( attrs.authtype == 'guest' ) {
                if( !$.trim( attrs.username ) ) {
                    var error = {};
                    error.authtype = attrs.authtype;
                    error.fieldId  = '#guest-email';
                    error.msg      = 'Please specify your email address';
                    errors = [ error ];
                }
            }
            else if( attrs.authtype == 'client' ) {
                if( !$.trim( attrs.username ) ) {
                    var error = {};
                    error.authtype = attrs.authtype;
                    error.fieldId  = '#user-email';
                    error.msg      = 'Please specify your email address';
                    errors = [ error ];
                }
                if( !$.trim( attrs.password ) ) {
                    var error = {};
                    error.authtype = attrs.authtype;
                    error.fieldId  = '#user-pass';
                    error.msg      = 'Please specify a password';
                    errors = errors || [];
                    errors.push( error );
                }                
            }

            if( errors ) { return errors; }
        }   
    } );

    Nac.SigninView = Backbone.View.extend( {

        template: _.template( $( '#signin-template' ).html() ),

        events: {
            'click #user-signin': 'userSignin',
            'focus #user-email': 'clearUserEmailError',
            'focus #user-pass': 'clearUserPassError',
            'click #guest-signin': 'guestSignin',
            'focus #guest-email': 'clearGuestError',
        },

        initialize: function() {
            this.listenTo( this.model, "invalid", this.updateErrors );
        },

        render: function() {
            $( this.el ).html( this.template() );
            return this
        },

        updateErrors: function( model, errors ) {
            _.each( errors, function( error ) {
                this.$( error.fieldId + '-control' ).addClass( 'error' );
            }, this );
            this.$( 'button' ).removeAttr( 'disabled' );                                    
            this.$( '.progress' ).hide();
        },

        clearGuestError: function() {
            this.$( '#guest-email-control' ).removeClass( 'error' );
        },

        clearUserErrors: function() {
            this.$( '#user-email-control' ).removeClass( 'error' );
            this.$( '#user-pass-control' ).removeClass( 'error' );            
        },

        clearUserEmailError: function() {
            this.$( '#user-email-control' ).removeClass( 'error' );
        },

        clearUserPassError: function() {
            this.$( '#user-pass-control' ).removeClass( 'error' );
        },

        userSignin: function( e ) {
            e.preventDefault();
            this.$( 'button' ).attr( 'disabled', 'disabled' );
            this.clearUserErrors();
            var username = this.$( '#user-email' ).val();
            var password = this.$( '#user-pass' ).val();
            this.$( '#users .progress' ).show();
            this.model.save({ 
                                authtype: 'client', 
                                username: username, 
                                password: password 
                            },
                            { 
                                success: function() { 
                                    window.location = 'http://www.tallac.com' 
                                },
                                error: function() { 
                                    $( 'button' ).removeAttr( 'disabled' );                                    
                                    $( '.progress' ).hide();
                                }  
                            } );
            return false;
        },

        guestSignin: function( e ) {
            e.preventDefault();
            this.$( 'button' ).attr( 'disabled', 'disabled' );
            this.clearGuestError();            
            var username = this.$( '#guest-email' ).val();
            this.$( '#guests .progress' ).show();
            this.model.save({ 
                                authtype: 'guest', 
                                username: username 
                            }, 
                            { 
                                success: function() { 
                                      window.location = 'http://www.tallac.com' 
                                },
                                error: function() { 
                                    $( 'button' ).removeAttr( 'disabled' );                                    
                                    $( '.progress' ).hide();
                                } 
                            } );
            return false;
        },
    } );

    $( '#content' ).html( new Nac.SigninView( { model: new Nac.Session() } ).render().el );

} );