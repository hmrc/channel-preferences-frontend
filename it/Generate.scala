/*
 * Copyright 2019 HM Revenue & Customs
 *
 */

import java.util.UUID

import uk.gov.hmrc.domain.{ Nino, SaUtr }

import scala.util.Random

object Generate {
  private val random = new Random()

  def nino = Nino(f"CE${random.nextInt(100000)}%06dD")
  def utr = SaUtr(UUID.randomUUID.toString)
  def entityId = UUID.randomUUID.toString
}
