package io.github.mlypik

import doobie.util.transactor.Transactor.Aux
import cats.implicits._
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
      accountid   BIGINT,
      balance BIGINT)""".update
    val insert = sql"""INSERT INTO account (accountId, balance) VALUES (1234, 100)""".update

    val task = (drop.run *> create.run *> insert.run).transact(xa).runAsync
    Await.result(task, 5.seconds)
  }

}
