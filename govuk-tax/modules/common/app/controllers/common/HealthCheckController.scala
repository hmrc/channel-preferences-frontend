package controllers.common

import play.api.mvc.{Action, Controller}
import play.api.Play
import com.typesafe.config.ConfigValueType
import play.api.libs.ws.WS
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.mutable.ListBuffer

class HealthCheckController extends Controller {

  import play.api.Play.current

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  private lazy val env = Play.mode

  private lazy val serviceConfig = Play.current.configuration.getConfig(s"govuk-tax.$env.services")

  private lazy val services = serviceConfig.get.underlying.root flatMap {
    case (k, v) => {
      v.valueType() match {
        case ConfigValueType.OBJECT => {
          Some((k, v.unwrapped))
        }
        case _ => None
      }
    }
  }

  def check() = Action {
    val errors = services.foldLeft(ListBuffer.empty[String]) {
      (errors, entry) => {
        val serviceName = entry._1
        val serviceDef = entry._2.asInstanceOf[java.util.HashMap[String,AnyRef]].asScala

        val host = serviceDef("host").asInstanceOf[String]
        val port = serviceDef("port").asInstanceOf[Int]

        pingService(serviceName, s"http://$host:$port/ping/ping") foreach { errors += _ }

        errors
      }
    }.toList

    if (errors.isEmpty) {
      Ok("")
    } else {
      InternalServerError(errors.mkString("\n"))
    }
  }

  def pingService(name: String, url: String): Option[String] = {
    try {
      val futureResponse = WS.url(url).withHeaders(("Accept", "text/plain")).get()
      val result = Await.result(futureResponse, Duration("30 seconds"))
      result.status match {
        case 200 => None
        case s => Some(s"$name:$url:${s.toString}")
      }
    } catch {
      case e: Exception => Some(s"$name:${e.getClass.getSimpleName}:${e.getMessage}")
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
