package views

import java.io.{ PrintWriter, FileWriter }
import play.api.templates.Html
import org.jsoup.nodes.{ Element, Document }
import org.jsoup.select.Elements
import org.jsoup.Jsoup

trait PageSugar {

  implicit def fromHtmlToPage(html: Html): Page = {
    Page(Jsoup.parse(html.body))
  }
}

case class Page(document: Document) {
  def apply(selector: String): Elements = {
    document.select(selector)
  }
}

