package controllers.common

import scala.concurrent.Future
import com.typesafe.config.ConfigValueType

import play.api.mvc.{Action, Controller}
import play.api.Play
import play.api.libs.ws.WS
import play.api.libs.json.Json

import uk.gov.hmrc.common.MdcLoggingExecutionContext._
import controllers.common.actions.{LoggingDetails, HeaderCarrier}
import controllers.common.service.RunMode

class HealthCheckController extends Controller with RunMode {


  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  private lazy val serviceConfig = Play.current.configuration.getConfig(s"govuk-tax.$env.services")

  private lazy val services: Map[String, Map[String, AnyRef]] = {
    val config = serviceConfig.get.underlying.root.flatMap {
      case (serviceName, v) => {
        v.valueType() match {
          case ConfigValueType.OBJECT => {
            Some((serviceName, v.unwrapped.asInstanceOf[java.util.HashMap[String, AnyRef]].asScala.toMap))
          }
          case _ => None
        }
      }
    }
    // The .toMap converts from mutable to immutable
    config.toMap
  }

  def check() = Action.async { implicit request =>
    implicit val loggingDetails = HeaderCarrier(request)

    val healthFutures = services.flatMap { service =>
      val (serviceName, serviceDef) = service

      for {
        host <- serviceDef.get("host")
        port <- serviceDef.get("port")
      } yield {
        pingService(serviceName, s"http://$host:$port/ping/ping")
      }
    }

    Future.sequence(healthFutures).map { errors =>
      errors.flatten match {
        case Nil => Ok("")
        case errors => InternalServerError(errors.mkString("\n"))
      }
    }
  }

  def pingService(name: String, url: String)(implicit ld: LoggingDetails): Future[Option[String]] = {
    val futureResponse = WS.url(url).withHeaders(("Accept", "text/plain")).get()
    futureResponse.map {
      _.status match {
        case 200 => None
        case s => Some(s"$name:$url:${s.toString}")
      }
    }.recover {
      case t: Throwable => Some(s"$name:${t.getClass.getSimpleName}:${t.getMessage}")
    }
  }

  def details() = Action {
    Ok(Json.toJson(manifest))
  }

  def detail(name: String) = Action {
    if (manifest.containsKey(name)) Ok(manifest(name)) else NotFound
  }

  private val resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF")

  private val manifest = resources.foldLeft(Map.empty[String, String]) { (map, url) =>
    val manifest = new java.util.jar.Manifest(url.openStream())
    if (map.isEmpty && isApplicationManifest(manifest)) {
      manifest.getMainAttributes.toMap.map {
        t => t._1.toString -> t._2.toString
      }
    } else {
      map
    }
  }

  private def isApplicationManifest(manifest: java.util.jar.Manifest) = {
    "govuk-tax".equals(manifest.getMainAttributes.getValue("Implementation-Title")) &&
      "uk.gov.hmrc".equals(manifest.getMainAttributes.getValue("Implementation-Vendor"))
  }
}
