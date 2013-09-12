package uk.gov.hmrc

object Transform {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._

  private implicit val formats = DefaultFormats

  def fromResponse[A](body: String)(implicit m: Manifest[A]): A = Option(body) match {
    case Some(b) if b.length > 0 => parse(b, useBigDecimalForDouble = true).extract
    case _ => throw new IllegalArgumentException("A string value is required for transformation")
  }

  def toRequestBody[A](obj: A): String = {
    compact(render(Extraction.decompose(obj)))
  }
}
