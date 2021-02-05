/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.channelpreferencesfrontend.config

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import com.typesafe.config.ConfigException
import play.api.{ Configuration, Environment }

class DIModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit =
    bindString(s"preferencesFrontendUrl", "preferencesFrontendUrl")

  protected def bindString(path: String, name: String): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(configuration.getOptional[String](path).getOrElse(configException(path)))

  private def resolveAnnotationName(path: String, name: String): String =
    name match {
      case "" => path
      case _  => name
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Throw"))
  private def configException(path: String) = throw new ConfigException.Missing(path)

  def env: Environment = environment
}
