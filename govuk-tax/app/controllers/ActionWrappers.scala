package controllers

import play.api.mvc._
import java.net.URI
import scala.concurrent.Future
import controllers.service.SamlForm

case class Person(uri: URI)

case class AuthenticatedPersonRequest[A](
  personUri: URI, private val request: Request[A]) extends WrappedRequest(request)

case class PersonRequest[A](
  person: Person, private val request: Request[A]) extends WrappedRequest(request)

trait ActionWrappers {

  self: Controller =>

  import scala.concurrent.ExecutionContext.Implicits._

  object AuthenticatedPersonAction {
    def apply(block: AuthenticatedPersonRequest[AnyContent] => PlainResult, samlForm: SamlForm = SamlForm()): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent, samlForm)(block)

    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm)(block: AuthenticatedPersonRequest[A] => PlainResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        val personUri: Option[String] = request.session.get("uri")
        personUri match {
          case Some(uri) => Future(block(AuthenticatedPersonRequest(URI.create(uri), request)))
          case _ => samlForm.get map { data =>
            self.Unauthorized(views.html.saml_auth_form(data.idaUrl, data.samlRequest))
          }
        }
      }
    }
  }

  object AuthenticatedBusinessAction {

  }
}
