package uk.gov.hmrc.common

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.domain.Email
import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}

object QueryBinders {

  implicit def stringToEmail(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[Email] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Email]] = {
      stringBinder.bind(key, params).map {
        case Right(string) =>
          try {
            Right(Email(string))
          } catch {
            case e: IllegalArgumentException =>
              Left("Not a valid email address")
          }
        case Left(f) => Left(f)
      }
    }

    override def unbind(key: String, email: Email): String = {
      stringBinder.unbind(key, email.value)
    }
  }
}

object FormBinders {

  implicit def numberFormatter: Formatter[Int] = new Formatter[Int] {

    override def bind(key: String, params: Map[String, String]): Either[Seq[FormError], Int] = {
      params.get(key).map {
        number =>
          try {
            Right(number.trim.toInt)
          } catch {
            case e: NumberFormatException => Left(Seq(FormError(key, "error.number")))
          }
      }.getOrElse(Left(Seq.empty))
    }

    override def unbind(key: String, value: Int) = Map(key -> value.toString)
  }

  val numberFromTrimmedString: Mapping[Int] = Forms.of[Int](numberFormatter)
}
