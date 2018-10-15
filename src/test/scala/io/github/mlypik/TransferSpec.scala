package io.github.mlypik

import akka.http.scaladsl.model._
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
        status shouldBe StatusCodes.NotFound

      }
    }
    "return account balance if account present (GET /balance/accountId)" in {
      val request = Get(uri = "/balance/1234")

      request ~> transferRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[AccountBalance] should ===(AccountBalance(1234, 100))
      }
    }
  }

}
