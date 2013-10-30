package controllers.bt.testframework.mocks

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.AffinityGroupParser
import play.api.mvc.Request

trait AffinityGroupParserMock extends MockitoSugar {

  val mockAffinityGroupParser = mock[MockableAffinityGroupParser]

  trait MockableAffinityGroupParser {
    def parseAffinityGroup(implicit request: Request[AnyRef]): String
  }

  trait MockedAffinityGroupParser {
    self: AffinityGroupParser =>
    override def parseAffinityGroup(implicit request: Request[AnyRef]): String = mockAffinityGroupParser.parseAffinityGroup
  }

}

