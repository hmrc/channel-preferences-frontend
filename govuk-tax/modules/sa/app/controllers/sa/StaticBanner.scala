package controllers.sa

object StaticHTMLBanner {

  def saPreferences() = """<div style="margin-left: 0.85em; padding: 10px 0;">
                             <a id="cta-setPreferences" href="{{insert link here}}"> set preferences</a>
                             <script>
                                     (function(window){
                                             /*
                                              *      get current url link and append to preferences endpoint which will
                                              *      serve as the redirection link
                                             */
                                             var preferencesEL = document.getElementById("cta-setPreferences"),
                                                     currentURL = window.location.href,
                                                     preferencesEndPoint = preferencesEL.attributes.href.value+'?rd='+encodeURIComponent(currentURL);
                                             preferencesEL.attributes.href.value = preferencesEndPoint;
                                     }(window));

                             </script>
                     </div>"""

}
