package io.github.mlypik


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val accountBalanceJsonFormat = jsonFormat2(AccountBalance)
}
