/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.filters

import akka.stream.Materializer
import javax.inject.{ Inject, Singleton }
import model.Encrypted
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ExceptionHandlingFilter @Inject()(
  val mat: Materializer
) extends Filter with Results {

  override def apply(action: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val ec = executionContextFromRequest(rh)

    action(rh) recoverWith {
      case unhealthyService: UnhealthyServiceException => Future.failed(unhealthyService)
      case e =>
        val urlBinder = implicitly[QueryStringBindable[Encrypted[String]]]
        urlBinder.bind("returnUrl", rh.queryString) match {
          case Some(Right(encryptedUrl)) =>
            Logger
              .error(message = "An error occurred when calling entity-resolver, redirecting to returnUrl.", error = e)
            Future.successful(Results.Redirect(encryptedUrl.decryptedValue))
          case _ => Future.failed(e)
        }
    }
  }

  private def executionContextFromRequest(rh: RequestHeader): ExecutionContext = {
    val hc = HeaderCarrierConverter.fromHeadersAndSession(rh.headers, Some(rh.session))
    MdcLoggingExecutionContext.fromLoggingDetails(hc)
  }
}
