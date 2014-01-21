package controllers.common.support

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import org.mockito.Matchers.{eq => meq, any}
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._


class TicketCacheSpec extends BaseSpec {


  "TicketCache" should {
    "stash the ticketId when it is provided" in new TicketCacheApplication {

      import ticketCache._

      when(keyStoreConnector.addKeyStoreEntry[StoredTicket](meq(actionId), meq(source), meq("formId"), meq(keyStoreData), meq(false))(any(classOf[Manifest[StoredTicket]]), any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = ticketCache.stashTicket(Some(TicketId(123)), "formId")
      await(result) shouldBe "stored"
    }

    "not the ticketId when it is not provided" in new TicketCacheApplication {
      val result = ticketCache.stashTicket(None, "formId")
      await(result) shouldBe "ignored"
    }

    "pop ticket found in key store" in new TicketCacheApplication {

      import ticketCache._

      when(keyStoreConnector.getEntry[StoredTicket](meq(actionId), meq(source), meq("formId"), meq(false))(any(classOf[Manifest[StoredTicket]]), any[HeaderCarrier])).
        thenReturn(Future.successful(Some(keyStoreData)))
      val result = ticketCache.popTicket("formId")
      await(result) shouldBe "123"
    }

    "pop ticket not found in key store" in new TicketCacheApplication {

      import ticketCache._

      when(keyStoreConnector.getEntry[StoredTicket](meq(actionId), meq(source), meq("formId"), meq(false))(any(classOf[Manifest[StoredTicket]]), any[HeaderCarrier])).
        thenReturn(Future.successful(None))
      val result = ticketCache.popTicket("formId")
      await(result) shouldBe "Unknown"
    }
  }

}

class TicketCacheApplication extends MockitoSugar {

  val keyStoreConnector = mock[KeyStoreConnector]
  val ticketCache = new TicketCache(keyStoreConnector)

  import ticketCache._

  type StoredTicket = Map[String, String]
  val keyStoreData: StoredTicket = Map(ticketKey -> "123")
}
