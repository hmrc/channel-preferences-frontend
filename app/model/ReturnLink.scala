package model

import play.api.libs.json.Json


case class ReturnLink(linkText: String, linkUrl: String)

object ReturnLink {
  def fromString(body: String) : ReturnLink = {
    val jsonBody = Json.parse(body)
    val returnLinkText = (jsonBody \ "returnLinkText").as[String]
    val returnUrl = (jsonBody \ "returnUrl").as[String]
    ReturnLink(returnLinkText, returnUrl)
  }
}
