package controllers.sa

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate

class ChangeAddressFormSpec extends BaseSpec with ShouldMatchers with MockitoSugar {

  private val fullForm = ChangeAddressForm(
    additionalDeliveryInfo = Some("info"),
    addressLine1 = Some("addr 1"),
    addressLine2 = Some("addr 2"),
    addressLine3 = Some("addr 3"),
    addressLine4 = Some("addr 4"),
    postcode = Some("SE1 1AA"))

  private def assertMatch(form: ChangeAddressForm, update: SaAddressForUpdate) {
    form.additionalDeliveryInfo shouldBe update.additionalDeliveryInfo
    form.addressLine1 shouldBe Option(update.addressLine1)
    form.addressLine2 shouldBe Option(update.addressLine2)
    form.addressLine3 shouldBe update.addressLine3
    form.addressLine4 shouldBe update.addressLine4
    form.postcode shouldBe update.postcode
  }

  "Converting a ChangeAddressForm to an SaAddressUpdate" should {

    "Return the correctly populated update object" in {
      assertMatch(fullForm, fullForm.toUpdateAddress)
    }

    "Throw an IllegalStateException if addressLine1 is None" in {
      val form = fullForm.copy(addressLine1 = None)
      evaluating(form.toUpdateAddress) should produce[IllegalStateException]
    }

    "Throw an IllegalStateException if addressLine2 is None" in {
      val form = fullForm.copy(addressLine2 = None)
      evaluating(form.toUpdateAddress) should produce[IllegalStateException]
    }

    "Allow None in any of the other fields" in {

      val form1 = fullForm.copy(additionalDeliveryInfo = None)
      assertMatch(form1, form1.toUpdateAddress)

      val form2 = fullForm.copy(addressLine3 = None)
      assertMatch(form2, form2.toUpdateAddress)

      val form3 = fullForm.copy(addressLine4 = None)
      assertMatch(form3, form3.toUpdateAddress)

      val form5 = fullForm.copy(postcode = None)
      assertMatch(form5, form5.toUpdateAddress)
    }

    "Allow None in all other fields" in {
      val form = fullForm.copy(additionalDeliveryInfo = None, addressLine3 = None, addressLine4 = None, postcode = None)
      assertMatch(form, form.toUpdateAddress)
    }
  }
}
