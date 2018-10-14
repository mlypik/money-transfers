package io.github.mlypik

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._

import scala.concurrent.Future

final case class MoneyTransfer(from: Long, to: Long)
final case class AccountBalance(accountId: Long, balance: Long)

class PersistenceHandler(transactor: Aux[IO, Unit]) {

  def getBalance(accountId: Long): Future[Either[Throwable, AccountBalance]] = {
    sql"""SELECT balance FROM account WHERE accountId = $accountId"""
      .query[Long]
      .unique
      .transact(transactor)
      .map(balance => AccountBalance(accountId, balance))
      .attempt
      .unsafeToFuture()
  }
}