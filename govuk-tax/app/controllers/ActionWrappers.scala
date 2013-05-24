package controllers

import play.api.mvc._
import java.net.URI
import scala.concurrent.Future
import controllers.service.SamlForm
import java.util.UUID
import play.api.Logger
import views.html.saml_auth_form

case class Person(uri: URI)

case class AuthenticatedPersonRequest[A](
  personId: UUID, private val request: Request[A]) extends WrappedRequest(request)

case class PersonRequest[A](
  person: Person, private val request: Request[A]) extends WrappedRequest(request)

trait ActionWrappers {

  self: Controller =>

  def Uuid(uuid: String): Option[UUID] = {
    try {
      Option(UUID.fromString(uuid))
    } catch {
      case _: IllegalArgumentException => Logger.warn(s"Invalid UUID '$uuid'"); None
    }
  }

  import scala.concurrent.ExecutionContext.Implicits._

  object AuthenticatedPersonAction {
    def apply(block: AuthenticatedPersonRequest[AnyContent] => PlainResult, samlForm: SamlForm = SamlForm()): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent, samlForm)(block)

    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm)(handler: AuthenticatedPersonRequest[A] => PlainResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        val result = for {
          id <- request.session.get("id")
          uuid <- Uuid(id)
        } yield (Future(handler(AuthenticatedPersonRequest(uuid, request))))
        result getOrElse {
          samlForm.get map { data =>
            self.Unauthorized(saml_auth_form(data.idaUrl, data.samlRequest))
          }
        }
      }
    }
  }

  object AuthenticatedBusinessAction {

  }
}
