import slick.jdbc.H2Profile.api._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object UpdateQuotes extends App {


  val db = Database.forConfig("h2file1")

  try {

    val stocks = TableQuery[Stocks]

    val allStocksQuery = stocks.result

    val res = Await.result(db.run(allStocksQuery), Duration.Inf)

    val symbols = res.map(_.symbol).toArray

    val newQuotes = YahooFinance.get(symbols.take(5)).asScala.values.map(_.getQuote)
    val quotesStrings = newQuotes.map { a =>
      //case class Quote(id: Option[Int], companyId: String, timestamp: String, price: Double, avgPrice50: Double, volume: Int, avgVolume: Int)
      Quote(None, "NASDAQ:" + a.getSymbol, a.getLastTradeTime.getTime.toString, a.getPrice, a.getPriceAvg50, a.getVolume, a.getAvgVolume)
    }


    val quotes = TableQuery[Quotes]
    val updateQuery = quotes ++= quotesStrings

    val f = db.run(updateQuery.map(a => println(s"Added $a Quotes")))
    Await.result(f, Duration.Inf)

    println("-----------------------------")


  } finally db.close


}
