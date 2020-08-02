package interfaces

import java.sql.Timestamp
import java.time.Clock

import helper.ConfigHelper.config
import com.typesafe.scalalogging.LazyLogging
import persistence.{Quote, Quotes, Stocks}
import slick.jdbc.H2Profile.api._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class YahooInterface extends LazyLogging {

  private val db = Database.forConfig("h2file1")

  private val fetchLimit = config.getInt("yahoo.fetchLimit")
  private val fetchIntervalInMinutes = config.getInt("yahoo.fetchIntervalInMinutes")
  private val timeToleranceInSeconds = config.getInt("timeToleranceInSeconds")

  def fetchQuotes = try {

    val allStocksQuery = TableQuery[Stocks].result
    val allStocksResult = Await.result(db.run(allStocksQuery), Duration.Inf)
    val places = allStocksResult.groupBy(_.place)

    places.foreach { case (place, stocks) =>

      val symbolsGrouped = stocks.map(_.symbol).grouped(fetchLimit)

      val quotesData = symbolsGrouped.toSeq.flatMap { symbols =>

        val stockQuotes = YahooFinance.get(symbols.toArray).asScala.values.map(_.getQuote)

        stockQuotes.map { a =>
          val stockId = s"$place : ${a.getSymbol}"
          val lastTrade = new Timestamp(a.getLastTradeTime.getTimeInMillis)
          val priceAvg = Option(a.getPriceAvg50).getOrElse(new java.math.BigDecimal(-1))
          Quote(None, stockId, lastTrade, a.getPrice, priceAvg, a.getVolume, a.getAvgVolume)
        }
      }

      val nowTime = Clock.systemUTC.instant
      val filteredQuotesData = quotesData.filter { data =>
        java.time.Duration.between(data.lastTrade.toInstant, nowTime).minusSeconds(timeToleranceInSeconds).toMinutes <= fetchIntervalInMinutes
      }

      if (filteredQuotesData.isEmpty) logger.warn("No new data available.") else {

        val quotes = TableQuery[Quotes]
        val updateQuery = quotes ++= filteredQuotesData

        val updateAction = db.run(updateQuery.map(a => logger.info(s"Added $a Interfaces.Quotes")))
        Await.result(updateAction, Duration.Inf)

      }
    }
  } finally db.close
}

object FetchQuotes extends App {
  new YahooInterface().fetchQuotes
}