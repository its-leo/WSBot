package interfaces

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId}

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import persistence.Tables._
import slick.jdbc.H2Profile.api._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MINUTES}

class YahooInterface extends LazyLogging {

  private val fetchLimit = config.getInt("yahoo.fetchLimit")
  private val fetchIntervalInMinutes = config.getInt("yahoo.fetchIntervalInMinutes")

  def fetchQuotes(place: String) = {

    val db = Database.forConfig("db")

    val quotes = TableQuery[Quotes]

    try {

      val allStocksQuery = TableQuery[Stocks].filter(_.place === place.toUpperCase).result
      val stocks = Await.result(db.run(allStocksQuery), Duration.Inf)
      assume(!stocks.isEmpty, s"No stocks for ${place.toUpperCase} available.")

      val timezone = config.getString(s"${place.toLowerCase}.timezone")

      val symbolsGrouped = stocks.map(_.symbol).grouped(fetchLimit)

      val newQuotes = symbolsGrouped.toSeq.flatMap { symbols =>

        val stockQuotes = YahooFinance.get(symbols.toArray).asScala.values.map(_.getQuote)

        stockQuotes.map { a =>
          val stockId = s"${place.toUpperCase} : ${a.getSymbol}"
          val lastTrade = a.getLastTradeTime.toInstant.atZone(ZoneId.of(timezone))
          val priceAvg = Option(a.getPriceAvg50).getOrElse(new java.math.BigDecimal(-1))
          Quote(None, stockId, lastTrade, a.getPrice, priceAvg, a.getVolume, a.getAvgVolume)
        }
      }

      val nowTime = Clock.systemUTC.instant.atZone(ZoneId.of(timezone))
      val filteredQuotesData = newQuotes.filter { data =>
        java.time.Duration.between(data.lastTrade, nowTime).toNanos < Duration(fetchIntervalInMinutes, MINUTES).toNanos
      }

      val updateQuery = (quotes ++= filteredQuotesData)

      val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss VV")
      val localTime = dateFormat.format(nowTime)
      val updateAction = db.run(updateQuery.map(a => logger.info(f"Added ${a.get}%4s Quotes at $localTime")))
      Await.result(updateAction, Duration.Inf)

    } finally db.close

  }
}