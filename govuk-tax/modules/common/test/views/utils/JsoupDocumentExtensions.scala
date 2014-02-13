package views.utils

import org.jsoup.nodes.{Element, Document}

object JsoupDocumentExtensions {

  implicit class JsoupDocumentWrapper(doc: Document) {
    def elementById(id: String): Option[Element] = Option(doc.getElementById(id))
    def elementTextForId(id: String): Option[String] = elementById(id).map(_.ownText)
  }
}
