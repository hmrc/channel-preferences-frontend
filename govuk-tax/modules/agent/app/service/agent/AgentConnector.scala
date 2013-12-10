package service.agent

import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.domain.Uar
import uk.gov.hmrc.common.microservice.agent.AgentConnectorRoot
import models.agent.{Client, MatchingPerson, SearchRequest, AgentRegistrationRequest}
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.http.Status
import play.api.Logger

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.Logger

class AgentConnector extends AgentConnectorRoot {

  def create(nino: String, newAgent: AgentRegistrationRequest)(implicit hc: HeaderCarrier): Future[Uar] = {
    httpPostF[Uar](s"/agent/register/nino/$nino", Json.parse(toRequestBody(newAgent))).map { uar =>
      uar.getOrElse(throw new RuntimeException("Unexpected error creating a new agent"))
    }
  }

  def searchClient(uri: String, searchRequest: SearchRequest)(implicit hc: HeaderCarrier): Future[Option[MatchingPerson]] = {
    httpPostF[MatchingPerson](uri, Json.parse(toRequestBody(searchRequest)))
  }

  def saveOrUpdateClient(uri: String, client: Client)(implicit hc: HeaderCarrier): Unit = {
    httpPost(uri, Json.parse(toRequestBody(client))) { response =>
      if(response.status != OK)
        Logger.error(s"Unexpected error saving the client, response status is: ${response.status} trying to hit: ${httpResource(uri)}")
    }
  }
}

object AgentConnector {
  def apply() = new AgentConnector()
}
