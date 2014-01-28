/*global _gaq, B64, Mdtpdf, $  */
(function ($, window, document) {
  "use strict";
  var GOVUK = GOVUK || {};

  GOVUK.toggleNonElectricFields = function ($form, electricChecked) {
    var $fieldsets = $form.find('.non-electric');

    if (electricChecked) {
        $fieldsets.addClass('js-hidden');
        $fieldsets.find(':text').val('');
        $fieldsets.find(':checked').prop('checked', false);
        $fieldsets.find('*[data-default]').prop('checked', true);
    } else {
        $fieldsets.removeClass('js-hidden');
        $fieldsets.find(':checked').prop('checked', false);
    }

  };
  GOVUK.toggleContextualFields = function () {
    var $DOM = $("#content"),
      setup = function () {
        //select 'yes' option when user selects a contextual input
        $DOM.on('focus', '.js-includes-contextual .js-input--contextual input, .js-includes-contextual .js-input--contextual select', function(e) {
            preselect($(e.currentTarget));
        });
        // if we select 'no' the contextual input values should be cleared
        $DOM.on('focus', '*[data-contextual-helper]', function () {
          toggle($(this));
        });
      },
      //useful if form validation had previously failed.
      preselect = function ($el) {
        $el.parents('.js-includes-contextual').find('*[data-contextual-helper="enable"]').trigger('click');
      },
      toggle = function ($el) {
        if ($el.data('contextual-helper') === "disable") {
          //clear value inputs
          $el.parents('.js-includes-contextual').find('.js-input--contextual').find(':input').each(function (i, el) {
            el.value = '';
            if($el.data('isenabled') === "undefined"){
                $(el).attr('data-isenabled', "disabled");
            } else {
                $(el).data('isenabled', "disabled")
            }
          });
        } else {
          var $inputs = $el.parents('.js-includes-contextual').find('.js-input--contextual').find(':input');
          //set focus on first input if its a text box
          $.each($inputs, function (index, element) {
            if (index === 0 && element.type === "text") {
                if( $(element).data('isenabled') !== "enabled") {
                    $(element).focus();
                    $(element).data('isenabled', "enabled");
                }
            }
          });
        }
      };
    return {
      setup: setup
    };
  }();

  GOVUK.questionnaireSubmission = function () {
      var $questionnaire = $('.questionnaire');
      $questionnaire.find('input[type=submit]').on("click", function(e) {
        $(this).parents('.questionnaire').toggleClass('js-hidden');
        e.preventDefault();
          var form = $("#form-end-journey-questionnaire");
          $.ajax({
                  type: "POST",
                  url: $(form).attr("action"),
                  data: $(form).serialize()
          });
      });
  };
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
  // allow the user to report page errors
GOVUK.ReportAProblem = function () {
    var $reportErrorContainer = $('.report-error__content'),
      $submitButton = $reportErrorContainer.find('.button'),
      showErrorMessage = function () {
      var link = '/beta-feedback-unauthenticated'
      if( $('#feedback-link').attr('href').length) {
        link = $('#feedback-link').attr('href')
      }
      var response = "<h2>Sorry, we're unable to receive your message right now.</h2> " +
                      "<p>We have other ways for you to provide feedback on the "  +
                      "<a href='"+ link + "'>support page</a>.</p>";

        $reportErrorContainer.html(response);
      },
      disableSubmitButton = function () {
        $submitButton.attr("disabled", true);
      },
      enableSubmitButton = function () {
        $submitButton.attr("disabled", false);
      },
      showConfirmation = function (data) {
			$reportErrorContainer.html(data.message);
      },
      submit = function (form, url) {
        $.ajax({
          type: "POST",
          url: url,
          datatype: 'json',
          data: $(form).serialize(),
          success: function (data) {
			showConfirmation(data);
          },
          error: function (jqXHR, status) {
            if (status === 'error' || !jqXHR.responseText) {
                showErrorMessage();
            }
          }
        });
      };
    return {
      submitForm: submit
    };
  }();

  var fingerprint = new Mdtpdf({
        screen_resolution: true
      }),
      encodedFingerPrint = B64.encode(fingerprint.get()),
      mdtpdfCookie = GOVUK.getCookie("mdtpdf");
  if (!mdtpdfCookie) {
    GOVUK.setCookie("mdtpdf", encodedFingerPrint, 7300);
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
    /*
    TODO: currently disabled until decision is made on analytics tool to use
    if ($target.data('regime')) {
      var regime = $(this).data('regime');
      _gaq.push(['_trackPageview', '/' + regime]);
    }
    */
  });
  /**
   * DOM ready
   */
  $(document).ready(function () {
    GOVUK.preventDoubleSubmit();
    //initialise stageprompt for Analytics
    //TODO: Enable once we set up Goggle Analytics
    //GOVUK.performance.stageprompt.setupForGoogleAnalytics();

    // toggle for reporting a problem (on all content pages)
      $('.report-error__toggle').on('click', function(e) {
        $('.report-error__content').toggleClass('js-hidden');
          e.preventDefault();
      });

      //feedback forms require a hidden field denoting if javascript is enabled

      var $feedbackForms = $('.form--feedback'),
          $errorReportForm = $('.report-error__content form');
      //we have javascript enabled so change hidden input to reflect this
      $feedbackForms.find('input[name="isJavascript"]').attr("value", true);

      //Initialise validation for the feedback form
      $errorReportForm.validate({
        errorClass: 'error-notification',
        errorPlacement: function(error, element) {
            error.insertBefore(element);
        },
        //Highlight invalid input
        highlight: function (element, errorClass) {
            $(element).parent().addClass('form-field--error');
        },
        //Unhighlight valid input
        unhighlight: function (element, errorClass) {
            $(element).parent().removeClass('form-field--error');
        },
        //When all fields are valid perform AJAX call
        submitHandler: function (form) {
           GOVUK.ReportAProblem.submitForm(form, $errorReportForm.attr("action"));
        }
      });


    $('.print-link a').attr('target', '_blank');

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

    if ($("*[data-contextual-helper]").length) {
      // setup showing/hiding of contextual fields
      GOVUK.toggleContextualFields.setup();
    }
      //toggle fuel questions depending on if user has selected zero emissions
      var $form = $("#add-car-benefit-fields");
      /**
       * if the server side validation returns an error and the user has already selected
       * an electric car. We need to hide the questions
       **/
      if ($form.find("#fuelType-electricity").prop("checked")) {
        GOVUK.toggleNonElectricFields($form, true);
        $form.data('electricFlagged', true);
      }
      $form.on('click', '*[data-iselectric]', function () {
        if ($(this).data("iselectric")) {
          GOVUK.toggleNonElectricFields($form, true);
          $form.data('electricFlagged', true);
        } else {
          GOVUK.toggleNonElectricFields($form, false);
          $form.removeData('electricFlagged');
        }
      });

      GOVUK.questionnaireSubmission();
  });
})(jQuery, window, document);