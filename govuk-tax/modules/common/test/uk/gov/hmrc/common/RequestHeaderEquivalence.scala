package uk.gov.hmrc.common

import org.scalautils.Equivalence
import play.api.mvc.RequestHeader

object RequestHeaderEquivalence extends Equivalence[RequestHeader] {

  def areEquivalent(h1: RequestHeader, h2: RequestHeader): Boolean = {
    h1.id == h2.id &&
    h1.tags == h2.tags &&
    h1.uri == h2.uri &&
    h1.path == h2.path &&
    h1.method == h2.method &&
    h1.version == h2.version &&
    h1.queryString == h2.queryString &&
    h1.headers.toMap == h2.headers.toMap &&
    h1.remoteAddress == h2.remoteAddress
  }
}
