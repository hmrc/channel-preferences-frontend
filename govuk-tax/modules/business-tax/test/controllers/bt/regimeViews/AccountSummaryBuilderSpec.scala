package controllers.bt.regimeViews

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.SaDomain.SaRoot
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import scala.util.{Success, Failure, Try}
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import org.mockito.Mockito._

class AccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  trait MockableBuilder {
    def buildAccountSummary(regimeRoot: SaRoot, buildPortalUrl: (String) => String): AccountSummary
    def oops(user: User): AccountSummary
  }

  trait Builder {

    val buildPortalUrl = mock[String => String]
    val saUtr = SaUtr("223333333")

    val mockUserAuthority = mock[UserAuthority]
    when(mockUserAuthority.saUtr).thenReturn(Some(saUtr))

    val saRoot = SaRoot(saUtr, Map("accountSummary" -> "/some/sa/root"))

    lazy val regimeRoot: Option[Try[SaRoot]] = Some(Success(saRoot))
    lazy val regimeRoots = RegimeRoots(sa = regimeRoot)

    lazy val user = User(userId = "john", userAuthority = mockUserAuthority, regimes = regimeRoots, decryptedToken = None)

    val mockBuilder = mock[MockableBuilder]

    val builder = new AccountSummaryBuilder[SaUtr, SaRoot] {

      override def buildAccountSummary(regimeRoot: SaRoot, buildPortalUrl: (String) => String): AccountSummary = {
        mockBuilder.buildAccountSummary(regimeRoot, buildPortalUrl)
      }

      override def oops(user: User): AccountSummary = {
        mockBuilder.oops(user)
      }

      override def rootForRegime(user: User): Option[Try[SaRoot]] = {
        regimeRoot
      }
    }
  }

  "AccountSummaryBuilder" should {

    "call oops if the root service call throws an exception" in new Builder {
      override lazy val regimeRoot = Some(Failure(new RuntimeException()))
      builder.build(buildPortalUrl, user)
      verify(mockBuilder).oops(user)
    }

    "call oops if the buildAccountSummary method throws an exception" in new Builder {
      when(mockBuilder.buildAccountSummary(saRoot, buildPortalUrl)).thenThrow(new NumberFormatException("broken"))
      builder.build(buildPortalUrl, user)
      verify(mockBuilder).oops(user)
    }

    "return the accountSummary from buildAccountSummary when it completes successfully" in new Builder {
      val accountSummary = AccountSummary("Some Regime", Seq.empty, Seq.empty)
      when(mockBuilder.buildAccountSummary(saRoot, buildPortalUrl)).thenReturn(accountSummary)
      builder.build(buildPortalUrl, user) shouldBe Some(accountSummary)
    }
  }
}
