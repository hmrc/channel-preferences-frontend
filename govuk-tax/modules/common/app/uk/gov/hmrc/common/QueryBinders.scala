package uk.gov.hmrc.common
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.domain.Email

object QueryBinders {


  implicit def stringToEmail(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[Email] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Email]] = {
      stringBinder.bind(key, params).map {
        case Right(string) if Email.isValid(string) =>
          println("accepting " + string)
          Right(Email(string))
        case _ =>
          println("rejecting")
          Left("Unable to bind a Pager")
      }
    }
    override def unbind(key: String, email: Email): String = {
      stringBinder.unbind(key, email.value)
    }
  }
}
