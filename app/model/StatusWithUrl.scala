/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package model
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }
import play.api.libs.json._

sealed trait StatusName extends EnumEntry with UpperSnakecase

object StatusName extends Enum[StatusName] with PlayJsonEnum[StatusName] {
  val values = findValues

  case object Paper extends StatusName
  case object EmailNotVerified extends StatusName
  case object BouncedEmail extends StatusName
//  case object WelshAvailable extends StatusName
  case object Alright extends StatusName
  case object NewCustomer extends StatusName
  case object NoEmail extends StatusName
}

sealed trait Category extends EnumEntry with UpperSnakecase

object Category extends Enum[Category] with PlayJsonEnum[Category] {
  import StatusName._
  val values = findValues

  case object ActionRequired extends Category
//  case object OptionAvailable extends Category
  case object Info extends Category

  private val statusByCategory: Map[Category, List[StatusName]] =
    Map(
      ActionRequired -> List(NewCustomer, Paper, EmailNotVerified, BouncedEmail, NoEmail),
//      OptionAvailable -> List(WelshAvailable),
      Info -> List(Alright)
    )
  private val categoryByStatus: Map[StatusName, Category] =
    for {
      (category, statuses) <- statusByCategory
      status               <- statuses
    } yield status -> category

  def apply(statusName: StatusName): Category = categoryByStatus(statusName)
}

case class PaperlessStatus(
  name: model.StatusName,
  category: model.Category,
  text: String
)

object PaperlessStatus {
  implicit val formats = Json.format[PaperlessStatus]
}

case class Url(
  link: String,
  text: String
)

object Url {
  implicit val formats = Json.format[Url]
}

case class StatusWithUrl(
  status: PaperlessStatus,
  url: Url
)

object StatusWithUrl {
  implicit val formats = Json.format[StatusWithUrl]
}
