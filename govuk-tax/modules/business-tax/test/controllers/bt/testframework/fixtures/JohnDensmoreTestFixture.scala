package controllers.bt.testframework.fixtures

import org.joda.time.{Duration, DateTimeZone, DateTime}
import controllers.domain.AuthorityUtils._
import scala.Some

trait JohnDensmoreTestFixture extends NonBusinessUserFixture {

  override val userId = "/auth/oid/densmore"
  override val authority = payeAuthority("userId", "CS700100A")

  override val currentTime: DateTime = new DateTime(2013, 9, 27, 15, 7, 22, 232, DateTimeZone.UTC)
  override val lastRequestTimestamp = Some(currentTime.minus(Duration.standardMinutes(1)))
  override val lastLoginTimestamp = Some(currentTime.minus(Duration.standardDays(14)))
}