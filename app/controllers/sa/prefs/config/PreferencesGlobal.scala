package controllers.sa.prefs.config

import config.FrontendGlobal
import controllers.sa.prefs.internal.OptInCohortConfigurationValues
import play.api.{Application, Configuration}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AuditConnector, ServicesConfig}

object PreferencesGlobal extends FrontendGlobal with ServicesConfig {

  val auditConnector: AuditConnector = AuditConnector

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
    OptInCohortConfigurationValues.verifyConfiguration()
  }

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"$env.microservice.metrics")
}
