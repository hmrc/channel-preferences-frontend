package controllers.bt.regimeViews

import views.helpers.RenderableMessage
//import scala.collection.mutable

case class AccountSummaries(regimes: Seq[AccountSummary])

case class AccountSummary(regimeName: String, messages: Seq[Msg], addenda: Seq[RenderableMessage])

case class Msg(messageKey: String, params: Seq[RenderableMessage] = Seq.empty)

//TODO - should we use a builder?
//object MessageBuilder {
//
//  class Builder {
//    val messages = mutable.MutableList[Msg]()
//
//    def and(messagekey: String, params: Seq[RenderableMessage] = Seq.empty): Builder = {
//      messages += Msg(messagekey, params)
//      this
//    }
//
//    def build = messages.toList
//  }
//
//  def apply = new Builder
//}


