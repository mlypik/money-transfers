package io.github.mlypik

import akka.actor.{Actor, ActorLogging, Props}
import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._

import scala.concurrent.Future

final case class MoneyTransfer(from: Long, to: Long)
final case class AccountBalance(accountId: Long, balance: Long)

object PersistenceActor {
  final case class GetBalance(accountId: Long)
  final case class PerformTransfer(transfer: MoneyTransfer)

  def props(transactor: Aux[IO, Unit]): Props = Props(new PersistenceActor(transactor))
}

class PersistenceActor(transactor: Aux[IO, Unit]) extends Actor with ActorLogging {
  import PersistenceActor._

  def receive: Receive = {
    case GetBalance(accountId) =>
      sender() ! getBalance(accountId)

    case PerformTransfer(_) =>
      sender() ! "OK"

  }

  def getBalance(accountId: Long): Future[AccountBalance] = {
    sql"""SELECT balance FROM account WHERE accountId = $accountId"""
      .query[Long]
      .unique
      .transact(transactor)
      .map(balance => AccountBalance(accountId, balance))
      .unsafeToFuture()
  }
}