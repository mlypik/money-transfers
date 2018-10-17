package io.github.mlypik

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfter, Matchers, WordSpec }

class TransferSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with TransferRoutes with BeforeAndAfter {

  import spray.json.DefaultJsonProtocol._

  val xa: Aux[Task, Unit] = Transactor.fromDriverManager[Task](
    "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  override val persistenceHandler: PersistenceHandler = new PersistenceHandler(xa)

  before {
    TestDataProvider.populateDatabase(xa)
  }

  "Get Balance" should {
    "return return not found if no account present (GET /balance/accountId)" in {
      val request = Get(uri = "/balance/9999")

      request ~> transferRoutes ~> check {
        status shouldBe NotFound
      }
    }
    "return account balance if account present (GET /balance/accountId)" in {
      val request = Get(uri = "/balance/1234")

      request ~> transferRoutes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        entityAs[AccountBalance] shouldBe AccountBalance(1234, 100)
      }
    }

    "Transfer" should {
      "perform basic money transfer between accounts (POST /transfer)" in {
        val transferSpec = MoneyTransfer(1234, 4321, 10)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe OK
        }

        Get(uri = "/balance/1234") ~> transferRoutes ~> check {
          status shouldBe OK
          entityAs[AccountBalance] shouldBe AccountBalance(1234, 90)
        }
        Get(uri = "/balance/4321") ~> transferRoutes ~> check {
          status shouldBe OK
          entityAs[AccountBalance] shouldBe AccountBalance(4321, 20)
        }
      }

      "fail if sender account does not exist(POST /transfer)" in {
        val transferSpec = MoneyTransfer(9999, 4321, 10)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe BadRequest
        }
      }

      "fail if recipient account does not exist(POST /transfer)" in {
        val transferSpec = MoneyTransfer(1234, 9999, 10)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe BadRequest
        }
      }

      "not allow overdraws (POST /transfer)" in {
        val transferSpec = MoneyTransfer(1234, 4321, 1000)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe BadRequest
        }
      }
    }

    "History" should {
      "show empty transaction history for account with no history (GET /history/accountId)" in {
        val historyRequest = Get(uri = "/history/1234")

        historyRequest ~> transferRoutes ~> check {
          status shouldBe OK
          entityAs[List[TransferRecord]] shouldBe empty
        }
      }
    }

    "History" should {
      "show transaction history for account with history (GET /history/accountId)" in {
        val transferSpec = MoneyTransfer(1234, 4321, 10)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe OK
        }

        val historyRequest = Get(uri = "/history/1234")

        historyRequest ~> transferRoutes ~> check {
          status shouldBe OK
          entityAs[List[TransferRecord]] should have length 1
        }
      }
      "be available for both accounts involved in transfer (GET /history/accountId)" in {
        val transferSpec = MoneyTransfer(1234, 4321, 10)
        val transferRequest = Post(uri = "/transfer", content = transferSpec)

        transferRequest ~> transferRoutes ~> check {
          status shouldBe OK
        }

        val historyRequestFrom = Get(uri = "/history/1234")

        historyRequestFrom ~> transferRoutes ~> check {
          status shouldBe OK
          val records = entityAs[List[TransferRecord]]
          records should have length 1
          val record = records.head

          record.accountId shouldBe 1234
          record.amount shouldBe -10
          record.ref shouldBe 4321
        }

        val historyRequestTo = Get(uri = "/history/4321")

        historyRequestTo ~> transferRoutes ~> check {
          status shouldBe OK
          val records = entityAs[List[TransferRecord]]
          records should have length 1
          val record = records.head

          record.accountId shouldBe 4321
          record.amount shouldBe 10
          record.ref shouldBe 1234
        }
      }
    }
  }
}

