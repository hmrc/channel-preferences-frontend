package controllers

import org.joda.time.LocalDate

trait DateFieldsHelper {

    def localDateToTuple(date : Option[LocalDate]) : (String, String, String) = {
      (date.map(_.getYear.toString).getOrElse(""),date.map(_.getMonthOfYear.toString).getOrElse(""),date.map(_.getDayOfMonth.toString).getOrElse(""))
    }

    def tupleToLocalDate(tupleDate : Option[(String,String,String)]) : Option[LocalDate] = {
      tupleDate match {
        case Some((y,m,d)) => try {
          Some(new LocalDate(y.toInt, m.toInt, d.toInt))
        } catch {
          case _: Throwable => None
        }
        case _ => None
      }
    }

    def buildDateFormField(fieldName: String, value : Option[(String, String, String)] ) : Seq[(String, String)] = {
      Seq(fieldName + "." + "day" -> value.map(_._3).getOrElse(""),
        fieldName + "." + "month" -> value.map(_._2).getOrElse(""),
        fieldName + "." + "year" -> value.map(_._1).getOrElse(""))
    }

}