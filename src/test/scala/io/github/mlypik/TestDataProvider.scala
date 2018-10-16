package io.github.mlypik

import doobie.util.transactor.Transactor.Aux
import doobie.implicits._
import monix.eval.Task
import scala.concurrent.Await
import scala.concurrent.duration._

import monix.execution.Scheduler.Implicits.global

object TestDataProvider {

  def populateDatabase(xa: Aux[Task, Unit]): Unit = {
    val drop = sql"""DROP TABLE IF EXISTS account""".update
    val create =
      sql"""CREATE TABLE account (
      accountid   BIGINT UNIQUE ,
      balance DECIMAL(20,4))""".update
    val insertAcc1234 = sql"""INSERT INTO account (accountId, balance) VALUES (1234, 100)""".update
    val insertAcc4321 = sql"""INSERT INTO account (accountId, balance) VALUES (4321, 10)""".update

    val program = for {
      _ <- drop.run
      _ <- create.run
      _ <- insertAcc1234.run
      _ <- insertAcc4321.run
    } yield ()

    val future = program.transact(xa).runAsync
    Await.result(future, 5.seconds)
  }

}
