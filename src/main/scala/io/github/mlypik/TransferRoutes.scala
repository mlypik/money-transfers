package io.github.mlypik

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import io.github.mlypik.PersistenceActor.GetBalance

import scala.concurrent.Future
import scala.concurrent.duration._

trait TransferRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[TransferRoutes])

  def persistenceActor: ActorRef

  implicit lazy val timeout = Timeout(5.seconds)

  lazy val transferRoutes: Route = {
    path("balance" / LongNumber) { accountId =>
      get {
        onSuccess(persistenceActor ? GetBalance(accountId)){
          case future: Future[Any] =>
            onSuccess(future) {
              case balance: AccountBalance =>
                complete(balance)
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
          case _ =>
            complete(StatusCodes.InternalServerError)

        }
      }
    }
  }

}
