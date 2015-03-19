package controllers.sa.prefs.config

import config.FrontendGlobal
import controllers.sa.prefs.internal.{CohortCalculator, OptInCohort}
import play.api.{Application, Configuration}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AuditConnector, ServicesConfig}

trait PreferencesGlobal extends FrontendGlobal with ServicesConfig {

  def cohortCalculator: CohortCalculator[OptInCohort]
  def auditConnector: AuditConnector

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
    cohortCalculator.verifyConfiguration()
  }

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration]
}

object PreferencesGlobal extends PreferencesGlobal  {
  val cohortCalculator = new CohortCalculator[OptInCohort] {
    override val values: List[OptInCohort] = OptInCohort.values
  }

  val auditConnector: AuditConnector = AuditConnector

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")
}
