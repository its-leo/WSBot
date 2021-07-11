package interfaces.traits

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Clock

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper._
import helper.YahooHelper
import persistence.Classes.Exchange
import persistence.PostgresProfile.api._
import persistence.Tables.{Stock, Stocks}

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Source

trait ExchangeInterface extends LazyLogging {

  val exchange: Exchange

  def mapLinesToStocks(lines: Seq[String]): Seq[Stock]

  def fetchStocks: Seq[Stock] = {

    val path = filePathFor(exchange)
    val url = stockUrlFor(exchange)

    val fileExists = path.toFile.exists
    lazy val fileLastModified = Files.getLastModifiedTime(path).toInstant
    lazy val fileAgeInDays = java.time.Duration.between(fileLastModified, Clock.systemDefaultZone.instant).toDays

    val fileContents = if (!fileExists || fileAgeInDays >= fetchIntervalInDaysFor(exchange)) {
      val source = Source.fromURL(url)
      val lines = try {
        val sourceLines = source.getLines.toSeq
        Files.write(path, sourceLines.mkString("\n").getBytes(StandardCharsets.UTF_8))
        sourceLines
      } finally source.close
      logger.info(s"Fetched ${exchange.name} data from $url")
      lines
    } else Files.readAllLines(path, StandardCharsets.UTF_8).asScala

    val newStocks = mapLinesToStocks(fileContents)

    val knownStocks = YahooHelper.filterKnown(newStocks)

    val db = Database.forConfig("db")

    try {

      val stocks = TableQuery[Stocks]

      val updateQuery = stocks.insertOrUpdateAll(knownStocks)
      val updatedStockSize = Await.result(db.run(updateQuery), 1.seconds).getOrElse(0)

      val knownQuote = ((updatedStockSize.toDouble) / newStocks.size * 100).round

      logger.info(s"Added $updatedStockSize known / ${newStocks.size} imported stocks ($knownQuote%) for ${exchange.name}.")

    } finally db.close

    knownStocks
  }
}