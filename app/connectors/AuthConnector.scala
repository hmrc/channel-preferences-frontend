package connectors

import play.api.libs.json._
import uk.gov.hmrc.domain.TaxIds._
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector => ExternalAuthConnector}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, Upstream4xxResponse}
import play.api.http.Status.UNAUTHORIZED
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.{WSGet, WSHttp}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait AuthConnector extends ExternalAuthConnector with ServicesConfig {
  def http: HttpGet

  def currentTaxIdentifiers(implicit hc: HeaderCarrier): Future[Set[TaxIdWithName]] = {
    import connectors.AuthConnector.ApiReads._
    http.GET[Set[TaxIdWithName]](s"$serviceUrl/auth/authority").recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == UNAUTHORIZED => Set()
    }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig with WSGet {
  lazy val authBaseUrl = baseUrl("auth")
  lazy val http: HttpGet = WsHttp
  lazy val hooks: Seq[HttpHook] = NoneRequired
//  lazy val serviceUrl: String = baseUrl("auth")
  lazy val serviceUrl: String = "localhost:8500"

  object ApiReads {

    import play.api.libs.functional.syntax._

    implicit val taxIdentifiersReads: Reads[Set[TaxIdWithName]] = (
    (__ \ "accounts" \ "sa" \ "utr").readNullable[SaUtr].orElse(Reads.pure(None)) and
    (__ \ "accounts" \ "paye" \ "nino").readNullable[Nino].orElse(Reads.pure(None))
    ) (toSet _)

    private def toSet(utr: Option[SaUtr], nino: Option[Nino]): Set[TaxIdWithName] = Set(utr, nino).flatten: Set[TaxIdWithName]
  }
}