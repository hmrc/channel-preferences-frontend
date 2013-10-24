package controllers.sa

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate

class ChangeAddressFormSpec extends BaseSpec with MockitoSugar {

  private val fullForm = ChangeAddressForm(
    addressLine1 = Some("addr 1"),
    addressLine2 = Some("addr 2"),
    addressLine3 = Some("addr 3"),
    addressLine4 = Some("addr 4"),
    postcode = Some("SE1 1AA"),
    additionalDeliveryInformation = Some("info"))

  private def assertMatch(form: ChangeAddressForm, update: SaAddressForUpdate) {
    form.addressLine1 shouldBe Option(update.addressLine1)
    form.addressLine2 shouldBe Option(update.addressLine2)
    form.addressLine3 shouldBe update.addressLine3
    form.addressLine4 shouldBe update.addressLine4
    form.postcode shouldBe update.postcode
    form.additionalDeliveryInformation shouldBe update.additionalDeliveryInformation
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

      val form3 = fullForm.copy(addressLine3 = None)
      assertMatch(form3, form3.toUpdateAddress)

      val form4 = fullForm.copy(addressLine4 = None)
      assertMatch(form4, form4.toUpdateAddress)

      val formP = fullForm.copy(postcode = None)
      assertMatch(formP, formP.toUpdateAddress)

      val formA = fullForm.copy(additionalDeliveryInformation = None)
      assertMatch(formA, formA.toUpdateAddress)
    }

    "Allow None in all other fields" in {
      val form = fullForm.copy(addressLine3 = None, addressLine4 = None, postcode = None, additionalDeliveryInformation = None)
      assertMatch(form, form.toUpdateAddress)
    }
  }
}
