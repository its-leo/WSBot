package interfaces.traits

import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Clock

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import helper.YahooHelper
import persistence.PostgresProfile.api._
import persistence.Tables.{Stock, Stocks}

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Source

trait ExchangeInterface extends LazyLogging {

  val exchangeName: String

  private lazy val url = new URL(config.getString(s"$exchangeName.url"))
  private lazy val path = Paths.get(config.getString("dataPath") + s"/$exchangeName.txt")
  private lazy val fetchIntervalInDays = config.getInt(s"$exchangeName.fetchIntervalInDays")

  def mapLinesToStocks(lines: Seq[String]): Seq[Stock]

  def fetchStocks = {

    val fileExists = path.toFile.exists
    lazy val fileLastModified = Files.getLastModifiedTime(path).toInstant
    lazy val fileAgeInDays = java.time.Duration.between(fileLastModified, Clock.systemDefaultZone.instant).toDays

    val fileContents = if (!fileExists || fileAgeInDays >= fetchIntervalInDays) {
      val lines = Source.fromURL(url).getLines.toSeq
      Files.write(path, lines.mkString("\n").getBytes(StandardCharsets.UTF_8))
      logger.info(s"Fetched $exchangeName data from $url")
      lines
    } else Files.readAllLines(path, StandardCharsets.UTF_8).asScala

    val newStocks = mapLinesToStocks(fileContents)

    val knownStocks = YahooHelper.filterKnown(newStocks)

    val db = Database.forConfig("db")

    try {

      val stocks = TableQuery[Stocks]

      val oldStocksSize = Await.result(db.run(stocks.result), Duration.Inf).size

      val actions = stocks.insertOrUpdateAll(knownStocks)
      val stocksSize = Await.result(db.run(actions), 1.seconds).getOrElse(0)

      val knownQuote = ((stocksSize.toDouble - oldStocksSize) / newStocks.size * 100).round

      logger.info(s"Added ${stocksSize - oldStocksSize} new known / ${newStocks.size} imported stocks ($knownQuote%) for $exchangeName.")

    } finally db.close

    knownStocks
  }
}