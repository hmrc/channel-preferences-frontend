package controllers

import play.api.mvc._
import controllers.service._
import play.api.mvc.AsyncResult
import controllers.domain.TaxRegime
import microservice.auth.domain.UserAuthority

//case class AuthenticatedRequest[A](authority: AuthorityData, private val request: Request[A]) extends WrappedRequest(request)

//case class PersonalRequest[A](paye: Option[PayeData], sa: Option[SelfAssessmentData], private val request: AuthenticatedRequest[A]) extends WrappedRequest(request)

trait ActionWrappers extends MicroServices {

  self: Controller =>


  type Links = Map[String, String]

  case class PayeDesignatoryDetails(name: String) {

  }

  case class PayeRoot(designatoryDetails: PayeDesignatoryDetails,
                      links: Links) {

  }

  case class Personal(paye: PayeRoot)

  case class User(userAuthority: UserAuthority,
                  payeRoot: Option[PayeRoot] = None)

  object AuthorisedForAction {

    def apply[A <: TaxRegime](f: (User => Request[AnyContent] => AsyncResult)): Action[AnyContent] = Action {
      request =>
      // Today, as there is no IDA we'll assume that you are John Densmore
      // He has a oid of /auth/oid/jdensmore, so we'll get that from the auth service
      // TODO: This will need to handle session management / authentication when we support IDA

        val userId = "/auth/oid/jdensmore"
        val userAuthority = authMicroService.authority(userId)

        val responses = userAuthority.regimes.map {
          case ("paye", uri) => payeMicroService.root(uri)
          case _ => None
        }.flatten.toMap

        // Now I have a map of paye -> Future[Response]


        f(User(payeRoot = responses.get("paye"),
        userAuthority = userAuthority))(request)
    }
  }


  //  object AuthenticatedAction {
  //    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult, samlForm: SamlForm = SamlForm(), authority: Authority = new Authority()): Action[AnyContent] =
  //      apply(BodyParsers.parse.anyContent, samlForm, authority)(handler)
  //
  //    def apply[A](bodyParser: BodyParser[A], samlForm: SamlForm, authority: Authority)(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
  //      def parser = bodyParser
  //
  //      def apply(request: Request[A]): AsyncResult = Async {
  //        request.session.get("id") match {
  //          case Some(id) => {
  //            authority.get(id) map {
  //              authority =>
  //                handler(AuthenticatedRequest(authority, request))
  //            }
  //          }
  //          case None => {
  //            samlForm.get map {
  //              data =>
  //                self.Unauthorized(saml_auth_form(data.idaUrl, data.samlRequest))
  //            }
  //          }
  //        }
  //      }
  //    }
  //  }
  //
  //  object StubAuthenticatedAction {
  //    def apply(handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
  //      apply(BodyParsers.parse.anyContent)(handler)
  //
  //    def apply[A](bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
  //      def parser = bodyParser
  //
  //      def apply(request: Request[A]): AsyncResult = {
  //        handler(AuthenticatedRequest(AuthorityData("/auth/oid/09809809809",
  //          Some(PersonalData(Some(URI.create("/personal/paye/PP000007A")), None)), None), request))
  //      }
  //    }
  //  }
  //
  //  object WithPersonalData {
  //    def apply[A](handler: PersonalRequest[A] => AsyncResult, personalTax: PersonalTax = new PersonalTax()): (AuthenticatedRequest[A]) => AsyncResult = (request: AuthenticatedRequest[A]) => {
  //      Async {
  //        Logger.debug(s"Handling request $request")
  //        request.authority match {
  //          case AuthorityData(_, Some(PersonalData(Some(paye), None)), None) => {
  //            personalTax.payeData(paye.toString) map {
  //              result => handler(PersonalRequest(Some(result), None, request))
  //            }
  //          }
  //          case AuthorityData(_, Some(PersonalData(None, Some(sa))), None) => {
  //            personalTax.saData(sa.toString) map {
  //              result => handler(PersonalRequest(None, Some(result), request))
  //            }
  //          }
  //          case AuthorityData(_, Some(PersonalData(Some(paye), Some(sa))), None) => {
  //            val payeFuture = personalTax.payeData(paye.toString)
  //            val saFuture = personalTax.saData(sa.toString)
  //            for {
  //              paye <- payeFuture
  //              sa <- saFuture
  //            } yield (handler(PersonalRequest(Some(paye), Some(sa), request)))
  //          }
  //          case _ => Future(self.Unauthorized("Not allowed here"))
  //        }
  //      }
  //    }
  //  }
  //
  //  object WithBusinessData {
  //
  //  }

}
