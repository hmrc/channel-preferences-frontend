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

package config

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import com.typesafe.config.ConfigException
import play.api.http.HttpConfiguration
import play.api.i18n.{ DefaultMessagesApiProvider, Langs }
import play.api.{ Configuration, Environment }

import javax.inject.{ Inject, Singleton }

class PreferencesFrontendModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  protected def bindString(path: String, name: String): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(configuration.getOptional[String](path).getOrElse(configException(path)))

  override def configure(): Unit = {
    bind(classOf[DefaultMessagesApiProvider]).to(classOf[CohortMessagesApiProvider])
    bindString(s"CPFUrl", "CPFUrl")
  }

  private def resolveAnnotationName(path: String, name: String): String =
    name match {
      case "" => path
      case _  => name
    }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Throw"))
  private def configException(path: String) = throw new ConfigException.Missing(path)
}

@Singleton
class CohortMessagesApiProvider @Inject() (
  environment: Environment,
  config: Configuration,
  langs: Langs,
  httpConfiguration: HttpConfiguration
) extends DefaultMessagesApiProvider(environment, config, langs, httpConfiguration) {

  override protected def loadAllMessages: Map[String, Map[String, String]] =
    super.loadAllMessages
      .map { case (k, v) => if (k == "default") (k, merge(v, loadMessages("ipage.messages"))) else (k, v) }
      .map { case (k, v) => if (k == "cy") (k, merge(v, loadMessages("ipage.messages.cy"))) else (k, v) }
      .map { case (k, v) => if (k == "default") (k, merge(v, loadMessages("tcpage.messages"))) else (k, v) }
      .map { case (k, v) => if (k == "cy") (k, merge(v, loadMessages("tcpage.messages.cy"))) else (k, v) }
      .map { case (k, v) => if (k == "default") (k, merge(v, loadMessages("reoptinpage.messages"))) else (k, v) }
      .map { case (k, v) => if (k == "cy") (k, merge(v, loadMessages("reoptinpage.messages.cy"))) else (k, v) }
      .map { case (k, v) => if (k == "default") (k, merge(v, loadMessages("survey.messages"))) else (k, v) }
      .map { case (k, v) => if (k == "cy") (k, merge(v, loadMessages("survey.messages.cy"))) else (k, v) }

  private def merge(m1: Map[String, String], m2: Map[String, String]): Map[String, String] =
    (m1.toSeq ++ m2.toSeq).groupBy(_._1).mapValues(_(0)._2)
}
