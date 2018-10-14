package io.github.mlypik

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.effect.IO
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class TransferSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with TransferRoutes with BeforeAndAfterAll {

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
  )

  override val persistenceActor: ActorRef =
    system.actorOf(PersistenceActor.props(xa), "persistence")

  lazy val routes = transferRoutes

  override def beforeAll() = {
    val drop = sql"""DROP TABLE IF EXISTS account""".update
    val create =
      sql"""CREATE TABLE account (
      accountid   BIGINT,
      balance BIGINT)""".update
    val insert = sql"""INSERT INTO account (accountId, balance) VALUES (1234, 100)""".update

    (drop.run *> create.run *> insert.run).transact(xa).unsafeRunSync()
  }


  "TransferRoutes" should {
    "return account balance if account present (GET /balance/accountId)" in {
      val request = Get(uri = "/balance/1234")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[AccountBalance] should ===(AccountBalance(1234, 100))
      }
    }

  }
}
