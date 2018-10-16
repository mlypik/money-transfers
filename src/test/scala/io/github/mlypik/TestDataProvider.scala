package io.github.mlypik

import doobie.util.transactor.Transactor.Aux
import doobie.implicits._
import monix.eval.Task
import scala.concurrent.Await
import scala.concurrent.duration._

import monix.execution.Scheduler.Implicits.global

object TestDataProvider {

  def populateDatabase(xa: Aux[Task, Unit]): Unit = {
    val dropAccount = sql"""DROP TABLE IF EXISTS accounts""".update
    val createAccount =
      sql"""CREATE TABLE accounts (
      accountid   BIGINT UNIQUE NOT NULL,
      balance DECIMAL(20,4) NOT NULL,
      PRIMARY KEY (accountid)
      )""".update
    val insertAcc1234 = sql"""INSERT INTO accounts (accountId, balance) VALUES (1234, 100)""".update
    val insertAcc4321 = sql"""INSERT INTO accounts (accountId, balance) VALUES (4321, 10)""".update

    val dropTransfers = sql"""DROP TABLE IF EXISTS transfers""".update
    val createTransfers =
      sql"""CREATE TABLE transfers (
      accountid BIGINT NOT NULL,
      amount DECIMAL(20,4) NOT NULL,
      ref BIGINT NOT NULL,
      transactiondate VARCHAR(255),
      FOREIGN KEY (accountid) REFERENCES accounts(accountid),
      FOREIGN KEY (ref) REFERENCES accounts(accountid)
      )""".update

    val program = for {
      _ <- dropAccount.run
      _ <- createAccount.run
      _ <- insertAcc1234.run
      _ <- insertAcc4321.run
      _ <- dropTransfers.run
      _ <- createTransfers.run
    } yield ()

    val future = program.transact(xa).runAsync
    Await.result(future, 5.seconds)
  }

}
