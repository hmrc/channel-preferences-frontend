package controllers

import org.joda.time.DateTime
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.microservice.domain._
import controllers.common._

class BusinessTaxController extends BaseController with ActionWrappers with CookieEncryption with SessionTimeoutWrapper {

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>

        val userAuthority = user.userAuthority
        val encodedGovernmentGatewayToken = user.decryptedToken.get
        val businessUser = BusinessUser(user.regimes, userAuthority.utr, userAuthority.vrn, user.nameFromGovernmentGateway.getOrElse(""), userAuthority.previouslyLoggedInAt, encodedGovernmentGatewayToken)

        Ok(views.html.business_tax_home(businessUser))

  })

}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

