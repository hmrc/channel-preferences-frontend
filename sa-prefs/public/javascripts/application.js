/**
 * Attach a one-time event handler for all global links
 */
$(document).on('click', 'a', function(e) {
  var $target = $(this),
      linkHost = this.host,
      a = document.createElement('a');

  a.href = ssoUrl;
  ssoHost = a.host;

  if(linkHost === ssoHost) {
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
});
