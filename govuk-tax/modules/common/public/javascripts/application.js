function toggleDefaultOptions( $form, $options, bool ) {
    if( bool ) {
        $options.prop( "checked", true ).parents( 'li' ).addClass( 'visuallyhidden' );

        $form.find( '#co2Figure' ).val( '' )
            .end().find( '#co2NoFigure' ).prop( "checked", false );
    } else {
           //check if electric has data flagged if true turn it off and reset everything
           if( typeof $form.data( "electricFlagged" ) !== 'undefined' ) {
             $options.prop( "checked", false ).parents( 'li' ).removeClass( 'visuallyhidden' );
             //remove the electric data

           }
    }
}

var toggleContextualFields = function(){
    var $DOM = $( "#content" ),
    setup = function(){
        var $contextualInput = $DOM.find( '.includes-contextual .input--contextual' ).find( ':input' );

        $DOM.find( '[data-contextual-helper="disable"]:checked' ).each( function(){
            toggle( $( this ) );
        });

        $DOM.on( 'click', '*[data-contextual-helper]',function( e ){
            toggle( $( this ) );
        });
    },
    toggle = function( el ) {
        if( el.data( 'contextual-helper' ) === "disable" ){
            el.parents( '.includes-contextual' ).find( '.input--contextual' ).find( ':input' ).prop( 'disabled', true );
        } else {
            el.parents( '.includes-contextual' ).find( '.input--contextual' ).find( ':input' ).prop( 'disabled', false  );
        }
    }

    return {
        setup: setup
    }
}();

var fingerprint = new Fingerprint({screen_resolution: window.screen}).getPluginsString();
console.log(fingerprint);

var setCookie = function( name, value, duration ) {
    if( duration ) {
        var date = new Date();
        date.setTime(date.getTime()+(duration*24*60*60*1000));
        var expires = "; expires="+date.toGMTString();
    } else {
        var expires = '';
    }
    console.log(value);
    document.cookie = name + "=" + encodeURIComponent( value ) + expires + "; path=/";
}

var getCookie = function( name ) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

// TODO: remove this if it isn't being used
var eraseCookie = function( name ) {
	createCookie(name,"",-1);
}

var cookie = getCookie("mdtpdf");
if ( ! cookie ) {
//    setCookie ( "mdtpdf", "true" );
    setCookie ( "mdtpdf", fingerprint );
}
//
//var c = getCookie("mdtpdf");
//c = !!c;
//console.log(c);

/**
 * Attach a one-time event handler for all global links
 */
$(document).on('click', 'a', function(e) {

  var $target = $(this),
      linkHost = ($(this).data('sso') === true) ? true : false,
      a = document.createElement('a');

  a.href = ssoUrl;
  ssoHost = a.host;

  if(linkHost) {
    var successful = true;

    $.ajax({
      url: '/ssoout',
      data: { destinationUrl : $target[0].href },
      type: 'GET',
      async: false,
      cache: false,
      success: function(data, status, jqXHR) {
	var form = document.createElement('form');
	form.method = 'POST';
	form.action = ssoUrl;

	var payload = document.createElement('input');
	payload.type = 'hidden';
	payload.name = 'payload';
	payload.value = data;

	document.body.appendChild(form);
	form.appendChild(payload);

	// POST form
	form.submit();
      },
      error: function() { successful = false; }
    });

    // cancel link click event if everything is successful
    return !successful;
  }

  if( $target.data('regime') ) {
    var regime = $(this).data('regime');
    _gaq.push(['_trackPageview', '/' + regime]);
  }
});

/**
 * DOM ready
 */
$(document).ready(function() {
  $('.print-link a').attr('target', '_blank');

  // header search toggle
  $('.js-header-toggle').on('click', function(e) {
    e.preventDefault();
    $($(e.target).attr('href')).toggleClass('js-visible');
    $(this).toggleClass('js-hidden');
  });

  var $searchFocus = $('.js-search-focus');
  if($searchFocus.val() !== ''){
    $searchFocus.addClass('focus');
  }
  $searchFocus.on('focus', function(e){
    $(e.target).addClass('focus');
  });
  $searchFocus.on('blur', function(e){
    if($searchFocus.val() === ''){
      $(e.target).removeClass('focus');
    }
  });

  if(window.location.hash && $(".design-principles").length != 1 && $('.section-page').length != 1) {
    contentNudge(window.location.hash);
  }

  $("nav").delegate('a', 'click', function(){
    var hash;
    var href = $(this).attr('href');
    if(href.charAt(0) === '#'){
      hash = href;
    }
    else if(href.indexOf("#") > 0){
      hash = "#" + href.split("#")[1];
    }
    if($(hash).length == 1){
      $("html, body").animate({scrollTop: $(hash).offset().top - $("#global-header").height()},10);
    }
  });

  function contentNudge(hash){
    if($(hash).length == 1){
      if($(hash).css("top") == "auto" || "0"){
        $(window).scrollTop( $(hash).offset().top - $("#global-header").height()  );
      }
    }
  }

  // hover, active and focus states for buttons in IE<8
  if (!$.support.leadingWhitespace) {
    $('.button').not('a')
      .on('click focus', function (e) {
        $(this).addClass('button-active');
      })
      .on('blur', function (e) {
        $(this).removeClass('button-active');
      });

    $('.button')
      .on('mouseover', function (e) {
        $(this).addClass('button-hover');
      })
      .on('mouseout', function (e) {
        $(this).removeClass('button-hover');
      });
  }

  // fix for printing bug in Windows Safari
  (function () {
    var windows_safari = (window.navigator.userAgent.match(/(\(Windows[\s\w\.]+\))[\/\(\s\w\.\,\)]+(Version\/[\d\.]+)\s(Safari\/[\d\.]+)/) !== null),
        $new_styles;

    if (windows_safari) {
      // set the New Transport font to Arial for printing
      $new_styles = $("<style type='text/css' media='print'>" +
                      "@font-face {" +
                      "font-family: nta !important;" +
                      "src: local('Arial') !important;" +
                      "}" +
                      "</style>");
      document.getElementsByTagName('head')[0].appendChild($new_styles[0]);
    }
  }());
  // if(window.GOVUK && GOVUK.userSatisfaction){
  //   GOVUK.userSatisfaction.randomlyShowSurveyBar();
  // }

  if ($("*[data-contextual-helper]").length) {
    // setup showing/hiding of contextual fields
    toggleContextualFields.setup();
    //toggle fuel quesitons depending on if user has selected zero emissions
      var $form            = $("#form-add-car-benefit"),
          $defaultOptions  = $form.find('*[data-default]');

    /**
      * if the server side validation returns an error and the user has already selected
      * an electric car. We need to hide the questions
     **/
    if($form.find("#fuelType-electricity").prop("checked")) {
        toggleDefaultOptions($form, $defaultOptions, true);
        $form.data('electricFlagged', true);
    }
    $form.on('click', '*[data-iselectric]', function(e) {
        if ($(this).data("iselectric")) {
             toggleDefaultOptions($form, $defaultOptions, true) ;

             $form.data('electricFlagged', true);
        } else {

            toggleDefaultOptions($form, $defaultOptions, false);
            $form.removeData('electricFlagged');

        }
    });
  }
});