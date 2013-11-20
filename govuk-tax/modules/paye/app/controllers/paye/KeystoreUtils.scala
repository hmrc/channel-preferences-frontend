package controllers.paye

import uk.gov.hmrc.common.microservice.domain.User

object KeystoreUtils {
  val source = "paye"
  def formId(prefix: String, user: User, taxYear: Int, employmentSequenceNumber: Int) =  s"$prefix:${user.oid}:$taxYear:$employmentSequenceNumber"
}
