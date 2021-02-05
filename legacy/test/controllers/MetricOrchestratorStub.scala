/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.metrix.{ MetricOrchestrationResult, MetricOrchestrator }

import scala.concurrent.Future

trait MetricOrchestratorStub extends MockitoSugar {
  val fakeMetricOrchestrationResult: MetricOrchestrationResult = new MetricOrchestrationResult {
    override def andLogTheResult(): Unit = ()
  }
  val mockMetricOrchestrator: MetricOrchestrator = mock[MetricOrchestrator]

  when(mockMetricOrchestrator.attemptToUpdateAndRefreshMetrics(any())(any()))
    .thenReturn(Future.successful(fakeMetricOrchestrationResult))
  when(mockMetricOrchestrator.attemptToUpdateRefreshAndResetMetrics(any())(any()))
    .thenReturn(Future.successful(fakeMetricOrchestrationResult))

}
