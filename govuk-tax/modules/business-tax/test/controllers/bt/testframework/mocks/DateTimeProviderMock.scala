package controllers.bt.testframework.mocks

import org.joda.time.DateTime
import config.DateTimeProvider

trait DateTimeProviderMock {

  def currentTime: DateTime

  trait MockedDateTimeProvider {

    self: DateTimeProvider =>

    override lazy val now: () => DateTime = () => currentTime
  }

}
