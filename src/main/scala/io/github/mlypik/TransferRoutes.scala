package io.github.mlypik

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete

trait TransferRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[TransferRoutes])

  def persistenceHandler: PersistenceHandler

  lazy val transferRoutes: Route = {
    path("balance" / LongNumber) { accountId =>
      get {
        complete(persistenceHandler.getBalance(accountId))
        }
      }
    }

}
