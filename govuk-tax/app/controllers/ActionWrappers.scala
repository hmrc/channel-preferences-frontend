package controllers

import play.api.mvc._
import scala.concurrent.Future
import controllers.service._
import views.html.saml_auth_form
import controllers.service.AuthorityData
import play.api.mvc.AsyncResult
import scala.Some
import java.net.URI

case class AuthenticatedRequest[A](
  authority: AuthorityData, private val request: Request[A]) extends WrappedRequest(request)

case class PersonalRequest[A](
  personal: PersonalData, private val request: AuthenticatedRequest[A]) extends WrappedRequest(request)

trait ActionWrappers {

  self: Controller =>

  import scala.concurrent.ExecutionContext.Implicits._

  object AuthenticatedAction {
    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult, samlForm: SamlForm = SamlForm(), authority: Authority = new Authority()): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent, samlForm, authority)(handler)

    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm, authority: Authority)(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        request.session.get("id") match {
          case Some(id) => {
            authority.get(id) map { authority =>
              handler(AuthenticatedRequest(authority, request))
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

  object StubAuthenticationAction {
    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent)(handler)

    def apply[A](bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = Async {
        Future(handler(AuthenticatedRequest(AuthorityData("/auth/oid/09809809809",
          Some(PersonalData(Some(URI.create("/personal/pid/65732682375")))), None), request)))
      }
    }
  }

  object WithPersonalData {
    def apply[A](handler: PersonalRequest[A] => AsyncResult): (AuthenticatedRequest[A]) => AsyncResult = (request: AuthenticatedRequest[A]) => {
      Async {
        request.authority.personal match {
          case Some(personalData) => Future(handler(PersonalRequest(personalData, request)))
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
