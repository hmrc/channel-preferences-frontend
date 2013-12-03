/*global _gaq, B64, Mdtpdf, $  */
(function () {
  "use strict";
  var GOVUK = GOVUK || {};
  GOVUK.toggleDefaultOptions = function ($form, $options, bool) {
    if (bool) {
      $options.prop("checked", true).parents('li').addClass('visuallyhidden');
      $form.find('#co2Figure').val('')
        .end().find('#co2NoFigure').prop("checked", false);
    } else {
      //check if electric has data flagged if true turn it off and reset everything
      if (typeof $form.data("electricFlagged") !== 'undefined') {
        $options.prop("checked", false).parents('li').removeClass('visuallyhidden');
        //remove the electric data
      }
    }
  };
  GOVUK.toggleContextualFields = function () {
    var $DOM = $("#content"),
      setup = function () {
        //select 'yes' option when user selects a contextual input
        $DOM.on('click', '.js-includes-contextual .js-input--contextual input', function (e) {
          preselect($(e.currentTarget));
        });
        $DOM.on('change', '.js-includes-contextual .js-input--contextual select', function (e) {
          preselect($(e.currentTarget));
        });
        $DOM.on('click', '*[data-contextual-helper]', function () {
          toggle($(this));
        });
      },
      preselect = function ($el) {
        $el.parents('.js-includes-contextual').find('*[data-contextual-helper="enable"]').trigger('click');
      },
      toggle = function ($el) {
        if ($el.data('contextual-helper') === "disable") {
          //clear value inputs
          $el.parents('.js-includes-contextual').find('.js-input--contextual').find(':input').each(function (i, el) {
            el.value = '';
          });
        } else {
          var $inputs = $el.parents('.js-includes-contextual').find('.js-input--contextual').find(':input');
          //set focus on first input if its a text box
          $.each($inputs, function (index, element) {
            if (index === 0 && element.type === "text") {
              $(element).focus();
            }
          });
        }
      };
    return {
      setup: setup
    };
  }();

  GOVUK.setCookie = function (name, value, duration) {
    var expires = '';
    if (duration) {
      var date = new Date();
      date.setTime(date.getTime() + (duration * 24 * 60 * 60 * 1000));
      expires = "; expires=" + date.toGMTString();
    }
    document.cookie = name + "=" + value + expires + "; path=/";
  };
  GOVUK.getCookie = function (name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0) === ' ') {
        c = c.substring(1, c.length);
      }
      if (c.indexOf(nameEQ) === 0) {
        return c.substring(nameEQ.length, c.length);
      }
    }
    return null;
  };
  // TODO: remove this if it isn't being used
  GOVUK.eraseCookie = function (name) {
    GOVUK.setCookie(name, "", -1);
  };
  GOVUK.preventDoubleSubmit = function () {
    $('form').on('submit', function () {
      if (typeof $.data(this, "disabledOnSubmit") === 'undefined') {
        $.data(this, "disabledOnSubmit", {
          submited: true
        });
        $('input[type=submit], input[type=button]', this).each(function () {
          $(this).attr("disabled", "disabled");
        });
        return true;
      } else {
        return false;
      }
    });
  };
  var fingerprint = new Mdtpdf({
    screen_resolution: true
  }).get();
  var encodedFingerPrint = B64.encode(fingerprint);
  var mdtpdfCookie = GOVUK.getCookie("mdtpdf");
  if (!mdtpdfCookie) {
    GOVUK.setCookie("mdtpdf", encodedFingerPrint);
  }
  /**
   * Attach a one-time event handler for all global links
   */
  $(document).on('click', 'a', function () {
    var $target = $(this),
      linkHost = ($(this).data('sso') === true) ? true : false,
      a = document.createElement('a');
    a.href = ssoUrl;
    var ssoHost = a.host;
    if (linkHost) {
      var successful = true;
      $.ajax({
        url: '/ssoout',
        data: {
          destinationUrl: $target[0].href
        },
        type: 'GET',
        async: false,
        cache: false,
        success: function (data, status, jqXHR) {
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
        error: function () {
          successful = false;
        }
      });
      // cancel link click event if everything is successful
      return !successful;
    }
    if ($target.data('regime')) {
      var regime = $(this).data('regime');
      _gaq.push(['_trackPageview', '/' + regime]);
    }
  });
  /**
   * DOM ready
   */
  $(document).ready(function () {
    $('.print-link a').attr('target', '_blank');
    // header search toggle
    $('.js-header-toggle').on('click', function (e) {
      e.preventDefault();
      $($(e.target).attr('href')).toggleClass('js-visible');
      $(this).toggleClass('js-hidden');
    });
    var $searchFocus = $('.js-search-focus');
    if ($searchFocus.val() !== '') {
      $searchFocus.addClass('focus');
    }
    $searchFocus.on('focus', function (e) {
      $(e.target).addClass('focus');
    });
    $searchFocus.on('blur', function (e) {
      if ($searchFocus.val() === '') {
        $(e.target).removeClass('focus');
      }
    });
    if (window.location.hash && $(".design-principles").length !== 1 && $('.section-page').length !== 1) {
      contentNudge(window.location.hash);
    }
    $("nav").delegate('a', 'click', function () {
      var hash;
      var href = $(this).attr('href');
      if (href.charAt(0) === '#') {
        hash = href;
      } else if (href.indexOf("#") > 0) {
        hash = "#" + href.split("#")[1];
      }
      if ($(hash).length === 1) {
        $("html, body").animate({
          scrollTop: $(hash).offset().top - $("#global-header").height()
        }, 10);
      }
    });

    function contentNudge(hash) {
      if ($(hash).length === 1) {
        if ($(hash).css("top") === "auto" || "0") {
          $(window).scrollTop($(hash).offset().top - $("#global-header").height());
        }
      }
    }
    // hover, active and focus states for buttons in IE<8
    if (!$.support.leadingWhitespace) {
      $('.button').not('a')
        .on('click focus', function () {
          $(this).addClass('button-active');
        })
        .on('blur', function () {
          $(this).removeClass('button-active');
        });
      $('.button')
        .on('mouseover', function () {
          $(this).addClass('button-hover');
        })
        .on('mouseout', function () {
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
    if ($("*[data-contextual-helper]").length) {
      // setup showing/hiding of contextual fields
      GOVUK.toggleContextualFields.setup();
      //toggle fuel quesitons depending on if user has selected zero emissions
      var $form = $("#form-add-car-benefit"),
        $defaultOptions = $form.find('*[data-default]');
      /**
       * if the server side validation returns an error and the user has already selected
       * an electric car. We need to hide the questions
       **/
      if ($form.find("#fuelType-electricity").prop("checked")) {
        GOVUK.toggleDefaultOptions($form, $defaultOptions, true);
        $form.data('electricFlagged', true);
      }
      $form.on('click', '*[data-iselectric]', function () {
        if ($(this).data("iselectric")) {
          GOVUK.toggleDefaultOptions($form, $defaultOptions, true);
          $form.data('electricFlagged', true);
        } else {
          GOVUK.toggleDefaultOptions($form, $defaultOptions, false);
          $form.removeData('electricFlagged');
        }
      });
    }
    GOVUK.preventDoubleSubmit();
  });
})();