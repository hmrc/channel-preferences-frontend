package stubs


import stubs.Page.StubbedPage

object Host {

  def ReturnPage = StubbedPage(
    title          = "Redirected Page",
    relativeUrl    = "some/other/page",
    name           = "HostStub.pageToReturnTo",
    responseBody   = "<html><head><title>Redirected Page</title></head></html>"
  )

  def returnLinkText = "This is return link"
}
