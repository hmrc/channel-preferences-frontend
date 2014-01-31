package controllers.bt.accountsummary

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import controllers.common.actions.HeaderCarrier
import scala.concurrent._
import controllers.domain.AuthorityUtils._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import scala.Some
import uk.gov.hmrc.common.microservice.auth.domain.SaAccount

class AccountSummaryBuilderSpec extends BaseSpec with MockitoSugar {

  trait MockableBuilder {
    def buildAccountSummary(regimeRoot: SaRoot, buildPortalUrl: (String) => String): Future[AccountSummary]

    def oops(user: User): AccountSummary
  }

  trait Builder {

    val buildPortalUrl = mock[String => String]
    val saUtr = SaUtr("223333333")

    val saRoot = SaRoot(saUtr, Map("accountSummary" -> "/some/sa/root"))

    lazy val regimeRoot: Option[SaRoot] = Some(saRoot)
    lazy val regimeRoots = RegimeRoots(sa = regimeRoot)

    lazy val user = User(userId = "john", userAuthority = saAuthority("john", "223333333"), regimes = regimeRoots, decryptedToken = None)

    val mockBuilder = mock[MockableBuilder]

    val regimeNameKey = "test.regime.name"

    val manageRegimeKey = "test.manage.regime"


    val builder = new AccountSummaryBuilder[SaUtr, SaRoot, SaAccount] {

      override def buildAccountSummary(regimeRoot: SaRoot, buildPortalUrl: (String) => String)(implicit headerCarrier: HeaderCarrier): Future[AccountSummary] = {
        mockBuilder.buildAccountSummary(regimeRoot, buildPortalUrl)
      }

      override def defaultRegimeNameMessageKey = regimeNameKey

      override def defaultManageRegimeMessageKey = manageRegimeKey

      override def rootForRegime(user: User): Option[SaRoot] = regimeRoot

      override def accountForRegime(user: User): Option[SaAccount] = Some(SaAccount("link", saUtr))
    }
  }

  "AccountSummaryBuilder" should {

    //    "call oops if the root service call throws an exception" in new Builder {
    //      override lazy val regimeRoot = Some(Failure(new RuntimeException()))
    //      val expectedSummary = AccountSummary(regimeNameKey, Seq(Msg(CommonBusinessMessageKeys.oopsMessage)), Seq.empty, SummaryStatus.oops)
    //      builder.build(buildPortalUrl, user) shouldBe Some(expectedSummary)
    //    }
    //
    //    "call oops if the buildAccountSummary method throws an exception" in new Builder {
    //      when(mockBuilder.buildAccountSummary(saRoot, buildPortalUrl)).thenThrow(new NumberFormatException("broken"))
    //      val expectedSummary = AccountSummary(regimeNameKey, Seq(Msg(CommonBusinessMessageKeys.oopsMessage)), Seq.empty, SummaryStatus.oops)
    //      builder.build(buildPortalUrl, user) shouldBe Some(expectedSummary)
    //    }

    "return the accountSummary from buildAccountSummary when it completes successfully" in new Builder {
      val accountSummary = AccountSummary("Some Regime", "", Seq.empty, Seq.empty, SummaryStatus.success)
      when(mockBuilder.buildAccountSummary(saRoot, buildPortalUrl)).thenReturn(accountSummary)
      builder.build(buildPortalUrl, user).map(f => await(f)) shouldBe Some(accountSummary)
    }
  }
}
