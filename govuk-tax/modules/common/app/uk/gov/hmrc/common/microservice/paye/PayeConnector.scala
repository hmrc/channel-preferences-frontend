package uk.gov.hmrc.common.microservice.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.{TaxRegimeConnector, MicroServiceConfig}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future

class PayeConnector extends TaxRegimeConnector[PayeRoot] {
  override val serviceUrl = MicroServiceConfig.payeServiceUrl

  def root(uri: String)(implicit hc: HeaderCarrier): Future[PayeRoot] =
    httpGetF[PayeRoot](uri).map(_.getOrElse(throw new IllegalStateException(s"Expected Paye root not found at URI '$uri'")))

  def addBenefits(uri: String,
                  version: Int,
                  employmentSequenceNumber: Int,
                  benefits: Seq[Benefit])(implicit hc: HeaderCarrier): Future[Option[AddBenefitResponse]] = {
    httpPostF[AddBenefitResponse, AddBenefit](uri, Some(AddBenefit(version, employmentSequenceNumber, benefits)))
  }

  def removeBenefits(uri: String, withdrawBenefitRequest: WithdrawnBenefitRequest)(implicit hc: HeaderCarrier): Future[Option[RemoveBenefitResponse]] = {
    httpPostF[RemoveBenefitResponse, WithdrawnBenefitRequest](uri, Some(withdrawBenefitRequest))
  }

  def version(uri: String)(implicit hc: HeaderCarrier): Future[Int] = {
    httpGetF[Int](uri).map(_.getOrElse(throw new IllegalStateException(s"Expected paye version number not found at URI '$uri'")))
  }

}

object PayeConnector {
  val calculationWithdrawKey: String = "withdraw"
}
