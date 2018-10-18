package io.github.mlypik

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val moneyTransferJsonFormat = jsonFormat3(MoneyTransfer)
  implicit val accountBalanceJsonFormat = jsonFormat2(AccountBalance)

  implicit val transferRecordJsonFormat = jsonFormat4(TransferRecord)
  implicit val transferRecordsJsonFormat = jsonFormat1(TransferRecords)
}
