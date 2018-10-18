# money-transfers
Money transfer service with REST API.
Bare-bones, simple implementation in Scala. Uses:

* akka-http with spray-json for REST service
* doobie + monix + H2 for persistence support
## How to run
Run DemoServer test App:
```
sbt:money-transfers> test:run io.github.mlypik.DemoServer
[info] Running io.github.mlypik.DemoServer io.github.mlypik.DemoServer
Server online at http://127.0.0.1:8080/
```
It should start the service. DemoServer will start with datastore populated with some test data. There will be 2 accounts to work with:

|Account ID|Balance|
|----------|-------|
|1234      |100    |
|4321      |10     |
 
 ## How to use
 The most basic functionality is checking the current account balance:
 ```
 $ curl -H "Accept: application/json" localhost:8080/balance/1234 
 {"accountId":1234,"balance":100.0000}
```

To perform the transfer, we can POST the transfer details:
```
$ curl -H "Content-Type: application/json" --request POST --data '{"from": 1234, "to": 4321, "amount": 10}' localhost:8080/transfer
OK
```
After that, we can check the balance again:
```
$ curl -H "Accept: application/json" localhost:8080/balance/1234
{"accountId":1234,"balance":90.0000}
```
We can verify the same using transaction history:
```
$ curl -H "Accept: application/json" localhost:8080/history/1234
{"transfers":[{"accountId":1234,"amount":-10.0000,"ref":4321,"transactiondate":"2018-10-18T11:40:55.882Z"}]}
```

And for the other account:
```
$ curl -H "Accept: application/json" localhost:8080/balance/4321
{"accountId":4321,"balance":20.0000}
$ curl -H "Accept: application/json" localhost:8080/history/4321
{"transfers":[{"accountId":4321,"amount":10.0000,"ref":1234,"transactiondate":"2018-10-18T11:40:55.882Z"}]}
```
   
