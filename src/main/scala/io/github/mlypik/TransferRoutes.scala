package io.github.mlypik

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import io.github.mlypik.errors.{ AccountNotFound, OverdrawViolation }

trait TransferRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[TransferRoutes])

  def persistenceHandler: PersistenceHandler

  lazy val transferRoutes: Route = {
    path("balance" / LongNumber) { accountId =>
      get {
        onSuccess(persistenceHandler.getBalance(accountId)) {
          case Right(balance) => complete(balance)
          case Left(AccountNotFound) => complete(StatusCodes.NotFound, "Account not found")
          case Left(exception) =>
            log.error(exception, "Unknown error")
            complete(StatusCodes.InternalServerError)
        }
      }
    }
  } ~
    path("transfer") {
      post {
        entity(as[MoneyTransfer]) { transferSpec =>
          onSuccess(persistenceHandler.preformTransfer(transferSpec)) {
            case Right(Done) => complete(StatusCodes.OK)
            case Left(AccountNotFound) => complete(StatusCodes.BadRequest, "No account")
            case Left(OverdrawViolation) => complete(StatusCodes.BadRequest, "Attempted to overdraw")
            case Left(exception) =>
              log.error(exception, "Unknown error")
              complete(StatusCodes.InternalServerError)
          }
        }
      }
    } ~
    path("history" / LongNumber) { accountId =>
      get {
        onSuccess(persistenceHandler.getHistory(accountId)) {
          case Right(transfers) => complete(transfers)
          case Left(exception) =>
            log.error(exception, "Unknown error")
            complete(StatusCodes.InternalServerError)
        }
      }
    }
}
