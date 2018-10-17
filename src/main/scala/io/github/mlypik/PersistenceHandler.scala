package io.github.mlypik

import java.time.Instant

import akka.Done
import doobie.util.transactor.Transactor.Aux
import monix.eval.Task
import doobie.implicits._
import doobie.util.invariant.UnexpectedEnd
import io.github.mlypik.errors._

import scala.concurrent.Future
import monix.execution.Scheduler.Implicits.global

final case class MoneyTransfer(from: Long, to: Long, amount: BigDecimal)

final case class AccountBalance(accountId: Long, balance: BigDecimal)

final case class TransferRecord(accountId: Long, amount: BigDecimal, ref: Long, transactiondate: String)

class PersistenceHandler(transactor: Aux[Task, Unit]) {

  def getBalance(accountId: Long): Future[Either[Throwable, AccountBalance]] = {
    sql"""SELECT balance FROM accounts WHERE accountId = $accountId"""
      .query[BigDecimal]
      .unique
      .transact(transactor)
      .map(balance => AccountBalance(accountId, balance))
      .attempt
      .runAsync
      .map(_.left.map {
        case UnexpectedEnd => AccountNotFound
        case unknownException => unknownException
      })
  }

  def preformTransfer(transferSpec: MoneyTransfer): Future[Either[Throwable, Done]] = {
    def updateAccountOrFail(accountId: Long, expectedBalance: BigDecimal, targetBalance: BigDecimal) = {
      sql"""UPDATE accounts SET balance = $targetBalance WHERE accountId = $accountId AND balance = $expectedBalance"""
        .update.run.map {
          case 1 => Done
          case _ => BalanceUpdateFailed
        }
    }

    def insertTransferDetails(record: TransferRecord) = {
      sql"""INSERT INTO transfers (accountId, amount, ref, transactiondate)
            VALUES (${record.accountId}, ${record.amount}, ${record.ref}, ${record.transactiondate})"""
        .update.run
    }

    val futureBalanceFrom = getBalance(transferSpec.from)
    val futureBalanceTo = getBalance(transferSpec.to)

    val currentBalances: Future[(Either[Throwable, AccountBalance], Either[Throwable, AccountBalance])] = for {
      balanceFrom <- futureBalanceFrom
      balanceTo <- futureBalanceTo
    } yield (balanceFrom, balanceTo)

    currentBalances.flatMap {
      case (Left(exception), _) => Future.successful(Left(exception))
      case (_, Left(exception)) => Future.successful(Left(exception))
      case (Right(balanceFrom), Right(balanceTo)) =>
        val balanceFromAfterTransfer = balanceFrom.balance - transferSpec.amount
        val balanceToAfterTransfer = balanceTo.balance + transferSpec.amount
        val updateFromAccount = updateAccountOrFail(transferSpec.from, balanceFrom.balance, balanceFromAfterTransfer)
        val updateToAccount = updateAccountOrFail(transferSpec.to, balanceTo.balance, balanceToAfterTransfer)
        val transactionDate = Instant.now().toString
        val insertFrom = insertTransferDetails(TransferRecord(transferSpec.from, -transferSpec.amount, transferSpec.to, transactionDate))
        val insertTo = insertTransferDetails(TransferRecord(transferSpec.to, transferSpec.amount, transferSpec.from, transactionDate))

        val program = for {
          _ <- updateFromAccount
          _ <- updateToAccount
          _ <- insertFrom
          _ <- insertTo
        } yield Done

        if (balanceFromAfterTransfer < 0) {
          Future.successful(Left(OverdrawViolation))
        } else {
          program.transact(transactor).attempt.runAsync
        }
    }
  }

  def getHistory(accountId: Long): Future[Either[Throwable, List[TransferRecord]]] = {
    sql"""SELECT accountid, amount, ref, transactiondate FROM transfers WHERE accountId = $accountId"""
      .query[TransferRecord]
      .to[List]
      .transact(transactor)
      .attempt
      .runAsync
  }

}