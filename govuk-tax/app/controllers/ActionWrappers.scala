package controllers

import play.api.mvc._
import scala.concurrent.Future
import controllers.service._
import views.html.saml_auth_form
import controllers.service.AuthorityData
import play.api.mvc.AsyncResult
import scala.Some
import java.net.URI
import play.api.Logger

case class AuthenticatedRequest[A](
  authority: AuthorityData, private val request: Request[A]) extends WrappedRequest(request)

case class PersonalRequest[A](
  paye: Option[PayeData], sa: Option[SelfAssessmentData], private val request: AuthenticatedRequest[A]) extends WrappedRequest(request)

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

  object StubAuthenticatedAction {
    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
      apply(BodyParsers.parse.anyContent)(handler)

    def apply[A](bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
      def parser = bodyParser

      def apply(request: Request[A]): AsyncResult = {
        handler(AuthenticatedRequest(AuthorityData("/auth/oid/09809809809",
          Some(PersonalData(Some(URI.create("/personal/paye/PP000007A")), None)), None), request))
      }
    }
  }

  object WithPersonalData {
    def apply[A](handler: PersonalRequest[A] => AsyncResult, personalTax: PersonalTax = new PersonalTax()): (AuthenticatedRequest[A]) => AsyncResult = (request: AuthenticatedRequest[A]) => {
      Async {
        Logger.debug(s"Handling request $request")
        request.authority match {
          case AuthorityData(_, Some(PersonalData(Some(paye), None)), None) => {
            personalTax.payeData(paye.toString) map { result => handler(PersonalRequest(Some(result), None, request)) }
          }
          case AuthorityData(_, Some(PersonalData(None, Some(sa))), None) => {
            personalTax.saData(sa.toString) map { result => handler(PersonalRequest(None, Some(result), request)) }
          }
          case AuthorityData(_, Some(PersonalData(Some(paye), Some(sa))), None) => {
            val payeFuture = personalTax.payeData(paye.toString)
            val saFuture = personalTax.saData(sa.toString)
            for {
              paye <- payeFuture
              sa <- saFuture
            } yield (handler(PersonalRequest(Some(paye), Some(sa), request)))
          }
          case _ => Future(self.Unauthorized("Not allowed here"))
        }
      }
    }
  }

  object WithBusinessData {

  }
}
