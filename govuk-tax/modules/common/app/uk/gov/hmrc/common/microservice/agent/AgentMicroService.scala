package uk.gov.hmrc.common.microservice.agent

import uk.gov.hmrc.microservice._
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.domain.Uar
import play.api.libs.ws.Response

class AgentMicroService(override val serviceUrl: String = MicroServiceConfig.agentServiceUrl) extends MicroService {

  def root(uri: String) = httpGet[AgentRoot](uri).getOrElse(throw new IllegalStateException(s"Expected Agent root not found at URI '$uri'"))

  def create(nino: String, newAgent: AgentRegistrationRequest): Uar = {
    val uar = httpPost[Uar](s"/agent/register/nino/$nino", Json.parse(toRequestBody(newAgent)))
    uar.getOrElse(throw new RuntimeException("Unexpected error creating a new agent"))
  }

  def searchClient(uri: String, searchRequest: SearchRequest): Option[MatchingPerson] = httpPost[MatchingPerson](uri, Json.parse(toRequestBody(searchRequest)))

  def saveOrUpdateClient(uri: String, client: Client) {
    val response: Response = httpPostSynchronous(uri, Json.parse(toRequestBody(client)))
    response.status match {
      case OK => {}
      case status: Int => throw MicroServiceException(s"Unexpected error saving the client, response status is: $status trying to hit: ${httpResource(uri)}", response)
    }
  }
}

trait AgentMicroServices {

  implicit lazy val agentMicroService = new AgentMicroService()
}
