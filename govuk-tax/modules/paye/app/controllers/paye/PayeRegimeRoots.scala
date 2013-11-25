package controllers.paye

trait PayeRegimeRoots {

  import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
  import controllers.common.actions.HeaderCarrier
  import uk.gov.hmrc.common.microservice.domain.RegimeRoots
  import controllers.common.service.Connectors.payeConnector

  protected def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): RegimeRoots = {
    RegimeRoots(
      paye = authority.regimes.paye map {
        uri => payeConnector.root(uri.toString)
      }
    )
  }
}
