package controllers

package object bt {

  val getSavePrefsCall = controllers.bt.prefs.routes.BizTaxPrefsController.submitPrefsForm()

  val getKeepPaperCall = controllers.bt.prefs.routes.BizTaxPrefsController.submitKeepPaperForm()

}
