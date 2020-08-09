package interfaces

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import persistence.PostgresProfile.api._
import persistence.Tables._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class YahooInterface extends LazyLogging {

  private val fetchLimit = config.getInt("yahoo.fetchLimit")

  def fetchQuotes(exchangeName: String) = {

    val db = Database.forConfig("db")

    val quotes = TableQuery[Quotes]

    try {

      val allStocksQuery = TableQuery[Stocks].filter(_.exchange === exchangeName.toUpperCase).result
      val stocks = Await.result(db.run(allStocksQuery), Duration.Inf)
      assume(!stocks.isEmpty, s"No stocks for ${exchangeName.toUpperCase} available.")

      val timezone = config.getString(s"${exchangeName.toLowerCase}.timezone")

      val symbolsGrouped = stocks.map(_.symbol).grouped(fetchLimit)

      val newQuotes = symbolsGrouped.toSeq.flatMap { symbols =>

        val stockQuotes = YahooFinance.get(symbols.toArray).asScala.values.map(_.getQuote)

        stockQuotes.map { a =>
          val lastTrade = a.getLastTradeTime.toInstant.atZone(ZoneId.of(timezone))
          val priceAvg = Option(a.getPriceAvg50).getOrElse(new java.math.BigDecimal(-1))
          Quote(a.getSymbol, lastTrade, a.getPrice, priceAvg, a.getVolume, a.getAvgVolume)
        }
      }

      val oldQuotesSize = Await.result(db.run(quotes.result), Duration.Inf).size

      val updateQuery = quotes.insertOrUpdateAll(newQuotes)
      val quotesSize = Await.result(db.run(updateQuery), Duration.Inf).getOrElse(0)

      val nowTime = ZonedDateTime.now(ZoneId.of(timezone))
      val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss VV")
      val localTime = dateFormat.format(nowTime)

      logger.info(f"Added ${quotesSize - oldQuotesSize}%4s Quotes at $localTime")

    } finally db.close

  }
}