package controllers.bt

import org.joda.time.DateTime
import uk.gov.hmrc.microservice.auth.domain._
import uk.gov.hmrc.microservice.domain._
import controllers.common._
import uk.gov.hmrc.common.PortalDestinationUrlBuilder

class BusinessTaxController extends BaseController with ActionWrappers with CookieEncryption with SessionTimeoutWrapper {

  def home = WithSessionTimeoutValidation(AuthorisedForGovernmentGatewayAction() {
    implicit user =>
      implicit request =>

        request.session
        val userAuthority = user.userAuthority
        val encodedGovernmentGatewayToken = user.decryptedToken.get
        val businessUser = BusinessUser(user.regimes, userAuthority.utr, userAuthority.vrn, userAuthority.ctUtr, userAuthority.empRef, user.nameFromGovernmentGateway.getOrElse(""), userAuthority.previouslyLoggedInAt, encodedGovernmentGatewayToken)

        val portalHref = PortalDestinationUrlBuilder.build(request, user)("saFileAReturn")
        Ok(views.html.business_tax_home(businessUser, portalHref))

  })

}

case class BusinessUser(regimeRoots: RegimeRoots, utr: Option[Utr], vrn: Option[Vrn], ctUtr: Option[Utr], empRef: Option[EmpRef], name: String, previouslyLoggedInAt: Option[DateTime], encodedGovernmentGatewayToken: String)

