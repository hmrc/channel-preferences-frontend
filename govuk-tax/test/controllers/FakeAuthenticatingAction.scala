package controllers

import play.api.mvc._
import play.api.mvc.Results._
import play.api.mvc.AsyncResult
import scala.concurrent.Future

//object FakeAuthenticatingAction {
//
//  import scala.concurrent.ExecutionContext.Implicits._
//
//  def apply(authorityData: AuthorityData, handler: AuthenticatedRequest[AnyContent] => AsyncResult): Action[AnyContent] =
//    apply(authorityData, BodyParsers.parse.anyContent)(handler)
//
//  def apply[A](authorityData: AuthorityData, bodyParser: BodyParser[A])(handler: AuthenticatedRequest[A] => AsyncResult): Action[A] = new Action[A] {
//    def parser = bodyParser
//
//    def apply(request: Request[A]): AsyncResult = Async {
//      Future(handler(AuthenticatedRequest(authorityData, request)))
//    }
//  }
//}

