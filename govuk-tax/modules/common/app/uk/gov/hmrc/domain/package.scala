package uk.gov.hmrc

import uk.gov.hmrc.common.crypto.Encrypted

package object domain {
  // Workaround for play route compilation bug https://github.com/playframework/playframework/issues/2402
  type EncryptedEmail = Encrypted[Email]
}
