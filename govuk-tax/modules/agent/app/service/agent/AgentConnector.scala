package service.agent

import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.domain.Uar
import play.api.libs.ws.Response
import uk.gov.hmrc.microservice.MicroServiceException
import uk.gov.hmrc.common.microservice.agent.AgentConnectorRoot
import models.agent.{Client, MatchingPerson, SearchRequest, AgentRegistrationRequest}
import controllers.common.actions.HeaderCarrier

class AgentConnector extends AgentConnectorRoot {

  def create(nino: String, newAgent: AgentRegistrationRequest)(implicit hc: HeaderCarrier): Uar = {
    val uar = httpPost[Uar](s"/agent/register/nino/$nino", Json.parse(toRequestBody(newAgent)))
    uar.getOrElse(throw new RuntimeException("Unexpected error creating a new agent"))
  }

  def searchClient(uri: String, searchRequest: SearchRequest)(implicit hc: HeaderCarrier): Option[MatchingPerson] = httpPost[MatchingPerson](uri, Json.parse(toRequestBody(searchRequest)))

  def saveOrUpdateClient(uri: String, client: Client)(implicit hc: HeaderCarrier) {
    val response: Response = httpPostSynchronous(uri, Json.parse(toRequestBody(client)))
    response.status match {
      case OK => {}
      case status: Int => throw MicroServiceException(s"Unexpected error saving the client, response status is: $status trying to hit: ${httpResource(uri)}", response)
    }
  }
}

object AgentConnector {
  def apply() = new AgentConnector()
}
