package uk.gov.hmrc.common.filters

import play.api.mvc.{SimpleResult, RequestHeader, Filter}
import scala.concurrent.Future

object SessionCryptoFilter extends Filter {

  override def apply(next: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    next(rh)
  }
}
