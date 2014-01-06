package uk.gov.hmrc.common.microservice

import play.api.libs.ws.{Response, WS}
import play.api.http.Status
import controllers.common.domain.Transform._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Logger
import scala.concurrent._
import play.api.libs.json.JsValue
import controllers.common.HeaderNames
import controllers.common.actions.HeaderCarrier

trait AsyncConnector extends Status with HeaderNames {
  protected val serviceUrl: String

  protected def httpResource(uri: String)(implicit headerCarrier: HeaderCarrier) = {
    Logger.info(s"Accessing backend service: $serviceUrl$uri")
    WS.url(s"$serviceUrl$uri").withHeaders(headerCarrier.headers: _*)
  }

  protected def httpGet[A](uri: String)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] =
    response[A](httpResource(uri).get, uri)(extractJSONResponse[A])

  //FIXME: Why is the body a JsValue? Why do we care what type it is

  protected def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    val wsResource = httpResource(uri)
    response[A](wsResource.withHeaders(headers.toSeq: _*).put(body), uri)(extractJSONResponse[A])
  }

  protected def httpPutNoResponse(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit headerCarrier: HeaderCarrier) = {
    val wsResource = httpResource(uri)
    response(wsResource.withHeaders(headers.toSeq: _*).put(body), uri)(extractNoResponse)
  }

  protected def httpPost[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    val wsResource = httpResource(uri)
    response[A](wsResource.withHeaders(headers.toSeq: _*).post(body), uri)(extractJSONResponse[A])
  }

  protected def httpPostSynchronous(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit headerCarrier: HeaderCarrier): Future[Response] = {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(body)
  }

  protected def httpPostAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit headerCarrier: HeaderCarrier) {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).post(body) onFailure { case throwable =>
      Logger.error(s"Async post to $uri failed", throwable)
    }
  }

  protected def httpPutAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit headerCarrier: HeaderCarrier) {
    val wsResource = httpResource(uri)
    wsResource.withHeaders(headers.toSeq: _*).put(body) onFailure { case throwable =>
      Logger.error(s"Async put to $uri failed", throwable)
    }
  }

  protected def httpDeleteAndForget(uri: String)(implicit headerCarrier: HeaderCarrier) {
    val wsResource = httpResource(uri)
    wsResource.delete() onFailure { case throwable =>
      Logger.error(s"Async delete to $uri failed", throwable)
    }
  }

  protected def extractJSONResponse[A](response: Response)(implicit m: Manifest[A]): A = {
    try {
      fromResponse[A](response.body)
    } catch {
      case e: Throwable => {
        throw new Exception("Malformed result", e)
      }
    }
  }

  protected def extractNoResponse(response: Response): Response = {
    response
  }

  protected def response[A](futureResponse: Future[Response], uri: String)(handleResponse: (Response) => A)(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    futureResponse map { res =>
      res.status match {
        case OK => Some(handleResponse(res))
        case CREATED => Some(handleResponse(res))
        //TODO: add some proper error handling - 204 or 404 are returned as None
        case NO_CONTENT => None
        case NOT_FOUND => None
        case BAD_REQUEST => throw MicroServiceException("Bad request", res)
        case UNAUTHORIZED => throw UnauthorizedException("Unauthenticated request", res)
        case FORBIDDEN => throw ForbiddenException("Not authorised to make this request", res)
        case CONFLICT => throw MicroServiceException("Invalid state", res)
        case x => throw MicroServiceException(s"Internal server error, response status is: $x trying to hit: ${httpResource(uri)}", res)
      }
    }
  }
}

