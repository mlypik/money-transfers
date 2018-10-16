package io.github.mlypik

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import monix.eval.Task
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class TransferSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
  with TransferRoutes with BeforeAndAfterAll {

  val xa: Aux[Task, Unit] = Transactor.fromDriverManager[Task](
    "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
  )

  override val persistenceHandler: PersistenceHandler = new PersistenceHandler(xa)

  override def beforeAll() = {
    TestDataProvider.populateDatabase(xa)
  }

  "TransferRoutes" should {
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
  }

}
