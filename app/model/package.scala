/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

import uk.gov.hmrc.emailaddress.EmailAddress

package object model {

  // Workaround for play route compilation bug https://github.com/playframework/playframework/issues/2402
  type EncryptedEmail = Encrypted[EmailAddress]
}
