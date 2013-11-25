package controllers.common

trait AllRegimeRoots {

  import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
  import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
  import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
  import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
  import uk.gov.hmrc.common.microservice.domain.RegimeRoots
  import controllers.common.actions.HeaderCarrier
  import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority

  import controllers.common.service.Connectors._

  protected def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): RegimeRoots = {
    val regimes = authority.regimes
    RegimeRoots(
      paye = regimes.paye map {
        uri => payeConnector.root(uri.toString)
      },
      sa = regimes.sa flatMap {
        uri => authority.saUtr map {utr => SaRoot(utr, saConnector.root(uri.toString))}
      },
      vat = regimes.vat flatMap {
        uri => authority.vrn map {vrn => VatRoot(vrn, vatConnector.root(uri.toString))}
      },
      epaye = regimes.epaye flatMap {
        uri => authority.empRef map {empRef => EpayeRoot(empRef, epayeConnector.root(uri.toString))}
      },
      ct = regimes.ct flatMap {
        uri => authority.ctUtr map {utr => CtRoot(utr, ctConnector.root(uri.toString))}
      },
      agent = regimes.agent map {
        uri => agentConnectorRoot.root(uri.toString)
      }
    )
  }
}
