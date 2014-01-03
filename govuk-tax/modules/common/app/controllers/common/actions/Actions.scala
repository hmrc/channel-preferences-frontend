package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain._
import controllers.common.{AuthenticationProvider, SessionTimeoutWrapper}
import scala.concurrent.Future

trait Actions
  extends MdcHeaders
  with RequestLogging
  with AuditActionWrapper
  with SessionTimeoutWrapper
  with UserActionWrapper {

  private type PlayRequest = (Request[AnyContent] => SimpleResult)
  private type AsyncPlayRequest = (Request[AnyContent] => Future[SimpleResult])
  private type PlayUserRequest = User => PlayRequest
  private type AsyncPlayUserRequest = User => AsyncPlayRequest

  type UserAction = User => Action[AnyContent]

  implicit def makeAction(body: PlayUserRequest): UserAction = (user: User) => Action(body(user))

  implicit def makeFutureAction(body: AsyncPlayUserRequest): UserAction = (user: User) => Action.async(body(user))

  class AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                        account: Option[TaxRegime],
                        redirectToOrigin: Boolean,
                        pageVisibility: PageVisibilityPredicate) {
    def apply(body: PlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, account, redirectToOrigin, pageVisibility, body)

    def async(body: AsyncPlayUserRequest): Action[AnyContent] = authorised(authenticationProvider, account, redirectToOrigin, pageVisibility, body)
  }


  def AuthorisedFor(account: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
  = new AuthenticatedBy(account.authenticationType, Some(account), redirectToOrigin, pageVisibility)

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
  = new AuthenticatedBy(authenticationProvider, None, redirectToOrigin, pageVisibility)

  object UnauthorisedAction {
    def apply(body: PlayRequest): Action[AnyContent]  = unauthedAction(Action(body))
    def async(body: AsyncPlayRequest): Action[AnyContent] = unauthedAction(Action.async(body))

    private def unauthedAction(body:Action[AnyContent]): Action[AnyContent] =
      storeHeaders {
        logRequest {
          WithRequestAuditing {
            body
          }
        }
      }
  }

  private def authorised(authenticationProvider: AuthenticationProvider,
                         account: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: UserAction) =
    storeHeaders {
      logRequest {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationProvider, account, redirectToOrigin) {
            user =>
              WithPageVisibility(pageVisibility, user) {
                implicit user =>
                  WithRequestAuditing(user) {
                    user => body(user)
                  }
              }
          }
        }
      }
    }
}


