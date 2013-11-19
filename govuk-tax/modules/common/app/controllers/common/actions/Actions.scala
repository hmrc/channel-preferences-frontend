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

  def AuthorisedFor(account: TaxRegime,
                    redirectToOrigin: Boolean = false,
                    pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
                   (body: PlayUserRequest) =
    authorised(account.authenticationType, Some(account), redirectToOrigin, pageVisibility, body)

  def AuthenticatedBy(authenticationProvider: AuthenticationProvider,
                      redirectToOrigin: Boolean = false,
                      pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
                     (body: PlayUserRequest) =
    authorised(authenticationProvider, None, redirectToOrigin, pageVisibility, body)

  def AsyncAuthenticatedBy(authenticationProvider: AuthenticationProvider,
                           redirectToOrigin: Boolean = false,
                           pageVisibility: PageVisibilityPredicate = DefaultPageVisibilityPredicate)
                          (body: AsyncPlayUserRequest) =
    asyncAuthorised(authenticationProvider, None, redirectToOrigin, pageVisibility, body)

  def UnauthorisedAction(body: PlayRequest) =
    storeHeaders {
      logRequest {
        WithRequestAuditing {
          Action(body)
        }
      }
    }

  private def authorised(authenticationProvider: AuthenticationProvider,
                         account: Option[TaxRegime],
                         redirectToOrigin: Boolean,
                         pageVisibility: PageVisibilityPredicate,
                         body: PlayUserRequest) =
    storeHeaders {
      logRequest {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationProvider, account, redirectToOrigin) {
            user =>
              WithPageVisibility(pageVisibility, user) {
                user =>
                  WithRequestAuditing(user) {
                    user => Action(body(user))
                  }
              }
          }
        }
      }
    }

  private def asyncAuthorised(authenticationProvider: AuthenticationProvider,
                              account: Option[TaxRegime],
                              redirectToOrigin: Boolean,
                              pageVisibility: PageVisibilityPredicate,
                              body: AsyncPlayUserRequest) =
    storeHeaders {
      logRequest {
        WithSessionTimeoutValidation {
          WithUserAuthorisedBy(authenticationProvider, account, redirectToOrigin) {
            user =>
              WithPageVisibility(pageVisibility, user) {
                user =>
                  WithRequestAuditing(user) {
                    user => Action.async(body(user))
                  }
              }
          }
        }
      }
    }
}


