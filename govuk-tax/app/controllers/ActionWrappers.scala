package controllers

import play.api.mvc._
import java.net.URI
import scala.concurrent.Future
import controllers.service.{ TaxUser, SamlForm }
import views.html.saml_auth_form
import play.api.mvc.AsyncResult
import scala.Some
import controllers.service.TaxUserView

case class AuthenticatedRequest[A](
  taxUserView: TaxUserView, private val request: Request[A]) extends WrappedRequest(request)

// temporary class - this should be replaced with the object representing the personal service root object
case class Person(uri: URI)

case class PersonRequest[A](
  person: Person, private val request: AuthenticatedRequest[A]) extends WrappedRequest(request)

trait ActionWrappers {

  self: Controller =>

  import scala.concurrent.ExecutionContext.Implicits._

  object AuthenticatedAction {
    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult, samlForm: SamlForm = SamlForm(), taxUser: TaxUser = TaxUser()): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent, samlForm, taxUser)(handler)

    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm, taxUser: TaxUser)(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        request.session.get("id") match {
          case Some(id) => {
            taxUser.get(id) map { user =>
              handler(AuthenticatedRequest(user, request))
            }
          }
          case None => {
            samlForm.get map { data =>
              self.Unauthorized(saml_auth_form(data.idaUrl, data.samlRequest))
            }
          }
        }
      }
    }
  }

  object WithPersonalData {
    def apply[A](handler: PersonRequest[A] => AsyncResult): (AuthenticatedRequest[A]) => AsyncResult = (request: AuthenticatedRequest[A]) => {
      Async {
        request.taxUserView.person match {
          case Some(uri) => Future(handler(PersonRequest(Person(uri), request)))
          case _ => Future(self.Unauthorized("Not allowed here"))
        }
      }
    }
  }

  object WithBusinessData {

  }

  object WithAgentData {

  }
}
