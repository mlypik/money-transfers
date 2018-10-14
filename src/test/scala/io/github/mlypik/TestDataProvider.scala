package io.github.mlypik

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import cats.implicits._
import doobie.implicits._

object TestDataProvider {

  def populateDatabase(xa: Aux[IO, Unit]): Unit = {
    val drop = sql"""DROP TABLE IF EXISTS account""".update
    val create =
      sql"""CREATE TABLE account (
      accountid   BIGINT,
      balance BIGINT)""".update
    val insert = sql"""INSERT INTO account (accountId, balance) VALUES (1234, 100)""".update

    (drop.run *> create.run *> insert.run).transact(xa).unsafeRunSync()
  }

}
