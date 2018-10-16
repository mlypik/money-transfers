package io.github.mlypik

import akka.Done
import doobie.util.transactor.Transactor.Aux
import monix.eval.Task
import doobie.imports._

import scala.concurrent.Future
import monix.execution.Scheduler.Implicits.global

final case class MoneyTransfer(from: Long, to: Long, amount: BigDecimal)

final case class AccountBalance(accountId: Long, balance: BigDecimal)

class PersistenceHandler(transactor: Aux[Task, Unit]) {

  def getBalance(accountId: Long): Future[Either[Throwable, AccountBalance]] = {
    sql"""SELECT balance FROM account WHERE accountId = $accountId"""
      .query[BigDecimal]
      .unique
      .transact(transactor)
      .map(balance => AccountBalance(accountId, balance))
      .attempt
      .runAsync
  }

  def preformTransfer(transferSpec: MoneyTransfer): Future[Either[Throwable, Done]] = {
    val futureBalanceFrom = getBalance(transferSpec.from)
    val futureBalanceTo = getBalance(transferSpec.to)

    val currentBalances: Future[(Either[Throwable, AccountBalance], Either[Throwable, AccountBalance])] = for {
      balanceFrom <- futureBalanceFrom
      balanceTo <- futureBalanceTo
    } yield (balanceFrom, balanceTo)

    currentBalances.flatMap {
      case (Left(exception), _) => Future.successful(Left(exception))
      case (_, Left(exception)) => Future.successful(Left(exception))
      case (Right(balanceFrom), Right(balanceTo)) => {
        val balanceFromAfterTransfer = balanceFrom.balance - transferSpec.amount
        val balanceToAfterTransfer = balanceTo.balance + transferSpec.amount
        val updateFromAccount =
          sql"""UPDATE account SET balance = ${balanceFromAfterTransfer} WHERE accountId = ${transferSpec.from} AND balance = ${balanceFrom.balance}"""
            .update.run
        val updateToAccount =
          sql"""UPDATE account SET balance = ${balanceToAfterTransfer} WHERE accountId = ${transferSpec.to} AND balance = ${balanceTo.balance}"""
            .update.run

        val program = for {
          _ <- updateFromAccount
          _ <- updateToAccount
        } yield Done

        program.transact(transactor).attempt.runAsync
      }
    }

  }
}