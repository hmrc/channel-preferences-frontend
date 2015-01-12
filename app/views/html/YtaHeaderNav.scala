package views.html;

import views.helpers.HeaderNav

object YtaHeaderNav {

def apply() = HeaderNav(
title = Some("Your tax account"),
showBetaLink = false,
links = Some(views.html.includes.yta_header_nav_links())
)
}
