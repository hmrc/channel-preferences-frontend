package controllers

import play.api.mvc._
import java.net.URI
import scala.concurrent.Future
import controllers.service.{ TaxUserView, TaxUser, SamlForm }
import java.util.UUID
import play.api.Logger
import views.html.saml_auth_form

case class Person(uri: URI)

case class AuthenticatedRequest[A](
  uuid: UUID, private val request: Request[A]) extends WrappedRequest(request)

case class PersonRequest[A](
  person: Person, private val request: AuthenticatedRequest[A]) extends WrappedRequest(request)

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

  object AuthenticatedAction {
    def apply(block: AuthenticatedRequest[AnyContent] => AsyncResult, samlForm: SamlForm = SamlForm()): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent, samlForm)(block)

    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm)(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        val result: Option[Future[Result]] = for {
          id <- request.session.get("id")
          uuid <- Uuid(id)
        } yield (Future(handler(AuthenticatedRequest(uuid, request))))

        result getOrElse {
          samlForm.get map { data =>
            self.Unauthorized(saml_auth_form(data.idaUrl, data.samlRequest))
          }
        }
      }
    }
  }

  object WithPersonData {
    def apply[A](block: PersonRequest[A] => AsyncResult, taxUser: TaxUser = TaxUser()): (AuthenticatedRequest[A]) => AsyncResult =
      apply(taxUser)(block)

    def apply[A](taxUser: TaxUser)(handler: PersonRequest[A] => AsyncResult): (AuthenticatedRequest[A]) => AsyncResult = (request: AuthenticatedRequest[A]) => {
      Async {
        val futureUserView: Future[TaxUserView] = taxUser.get(request.uuid.toString)

        val result: Future[Result] = for {
          userView: TaxUserView <- futureUserView
          res <- userView.person match {
            case Some(uri) => Future(handler(PersonRequest(Person(uri), request)))
            case _ => Future(self.Unauthorized("Not allowed here"))
          }
        } yield res

        result
      }
    }
  }

  object AuthenticatedBusinessAction {

  }
}
