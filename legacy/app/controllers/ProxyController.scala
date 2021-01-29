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

package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import connectors.OutboundProxyConnector
import org.slf4j.MDC
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class ProxyController @Inject()(
  outboundConnector: OutboundProxyConnector,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  val log: Logger = Logger(this.getClass)

  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
  val streamedBodyParser: BodyParser[Source[ByteString, Any]] =
    BodyParser(_ => Accumulator.source[ByteString].map((x: Source[ByteString, Any]) => Right.apply(x)))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def proxy2(predicate: String, path: String): Action[Source[ByteString, _]] = proxy(predicate + path)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def proxy(path: String): Action[Source[ByteString, _]] =
    Action.async(streamedBodyParser) { implicit request =>
      populateMdc(request)

      log.debug(s"Inbound Request: ${request.method} ${request.uri}")

      outboundConnector.proxy(request).recover {
        case ex: Exception =>
          log.error(s"An error occurred proxying $path", ex)
          InternalServerError(ex.getMessage)
      }
    }

  private def populateMdc(implicit request: Request[Source[ByteString, _]]): Unit = {
    val extraDiagnosticContext = Map(
      "transaction_id"                                         -> randomUUID.toString
    ) ++ request.headers.get(USER_AGENT).toList.map(USER_AGENT -> _)

    (hc.mdcData ++ extraDiagnosticContext).foreach {
      case (k, v) => MDC.put(k, v)
    }
  }

}
