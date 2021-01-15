/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  private def resolveAnnotationName(path: String, name: String): String = name match {
    case "" => path
    case _  => name
  }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Throw"))
  private def configException(path: String) = throw new ConfigException.Missing(path)

  def env: Environment = environment
}
