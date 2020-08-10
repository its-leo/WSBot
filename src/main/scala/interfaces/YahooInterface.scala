package interfaces

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper._
import persistence.Classes.Exchange
import persistence.PostgresProfile.api._
import persistence.Tables._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class YahooInterface extends LazyLogging {

  def fetchQuotes(exchange: Exchange): Unit = {

    val db = Database.forConfig("db")

    val quotes = TableQuery[Quotes]

    try {

      val allStocksQuery = TableQuery[Stocks].filter(_.exchange === exchange.name.toUpperCase).result

      val stocks = Await.result(db.run(allStocksQuery), Duration.Inf)

      assume(!stocks.isEmpty, s"No stocks for ${exchange.name.toUpperCase} available.")

      val symbolsGrouped = stocks.map(_.symbol).grouped(fetchLimit)

      val newQuotes = symbolsGrouped.toSeq.flatMap { symbols =>

        val stockQuotes = YahooFinance.get(symbols.toArray).asScala.values.map(_.getQuote)

        stockQuotes.collect { case a if a != null =>
          val lastTrade = a.getLastTradeTime.toInstant.atZone(zoneIdOf(exchange))
          val avgPrice = Option(a.getPriceAvg50).getOrElse(new java.math.BigDecimal(-1))

          Quote(stockId = a.getSymbol,
            lastTrade = lastTrade,
            price = a.getPrice,
            avgPrice50 = avgPrice,
            volume = a.getVolume,
            avgVolume = a.getAvgVolume)
        }
      }

      val updateQuery = quotes.insertOrUpdateAll(newQuotes)
      val quotesSize = Await.result(db.run(updateQuery), Duration.Inf).getOrElse(0)

      val nowTime = ZonedDateTime.now(zoneIdOf(exchange))
      val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss VV")
      val localTime = dateFormat.format(nowTime)

      logger.info(f"Added $quotesSize%4s Quotes for ${exchange.name}%6s at $localTime.")

    } finally db.close

  }
}