package interfaces

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper._
import helper.MathHelper.agdStringSeq
import persistence.Classes.Exchange
import persistence.PostgresProfile.api._
import persistence.Tables.{Stocks, _}
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class YahooInterface extends LazyLogging {

  def fetchQuotes(exchange: Exchange): Unit = {

    val db = Database.forConfig("db")

    val quotes = TableQuery[Quotes]

    try {

      val stocksQuery = TableQuery[Stocks]
      val allStocksQuery = stocksQuery.filter(_.exchange === exchange.name.toUpperCase).result

      val stocks = Await.result(db.run(allStocksQuery), Duration.Inf)

      assume(stocks.nonEmpty, s"No stocks for ${exchange.name.toUpperCase} available.")

      val symbolsGrouped = stocks.map(_.symbol).toSet.grouped(fetchLimit)

      val corruptSymbol: ListBuffer[String] = mutable.ListBuffer.empty

      val newQuotes = symbolsGrouped.toSeq.flatMap { symbols =>

        val stockQuotes = YahooFinance.get(symbols.toArray).asScala.values

        stockQuotes.map { case stock => try {

          val quote = stock.getQuote

          val lastTrade = quote.getLastTradeTime.toInstant.atZone(zoneIdOf(exchange))
          val avgPrice = Option(quote.getPriceAvg50).getOrElse(new java.math.BigDecimal(-1))

          Some(Quote(stockId = quote.getSymbol,
            lastTrade = lastTrade,
            price = quote.getPrice,
            avgPrice50 = avgPrice,
            volume = quote.getVolume,
            avgVolume = quote.getAvgVolume))
        } catch {
          case e: Exception => {
            corruptSymbol += stock.getSymbol
            None
          }
        }
        }
      }.collect { case a if (a.isDefined) => a.get }

      val deleteCorruptStocksQuery = stocksQuery.filter(_.id.inSet(corruptSymbol)).delete
      val deleted = Await.result(db.run(deleteCorruptStocksQuery), Duration.Inf)


      val updateQuery = quotes.insertOrUpdateAll(newQuotes)
      val quotesSize = Await.result(db.run(updateQuery), Duration.Inf).getOrElse(0)

      val nowTime = ZonedDateTime.now(zoneIdOf(exchange))
      val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss VV")
      val localTime = dateFormat.format(nowTime)

      logger.info(f"Added $quotesSize%4s Quotes for ${exchange.name}%6s at $localTime.")
      if(deleted > 0) logger.warn(s"Not added: ${corruptSymbol.mkExcerpt()}")

    } finally db.close

  }
}

object YahooInterface extends App {

 /* DatabaseHelper.delete[Stocks] _*/
  /*new XetraInterface().fetchStocks
  new YahooInterface().fetchQuotes(Exchange("xetra"))*/
}