import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object DeleteAllData extends App {

  val db = Database.forConfig("h2file1")

  try {

    val quotes = TableQuery[Quotes]
    val deleteQuotesAction = db.run(quotes.delete).map(numDeletedRows => println(s"Deleted $numDeletedRows Quotes"))
    Await.result(deleteQuotesAction, Duration.Inf)

/*
    val stocks = TableQuery[Stocks]
    val deleteStocksAction = db.run(stocks.delete).map(numDeletedRows => println(s"Deleted $numDeletedRows Stocks"))
    Await.result(deleteStocksAction, Duration.Inf)
*/
  } finally db.close
}