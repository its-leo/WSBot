package interfaces

import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Clock

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import persistence.{Stock, Stocks}
import slick.jdbc.H2Profile.api._

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Source

class NasdaqInterface extends LazyLogging {

  private val nasdaqPath = Paths.get(config.getString("nasdaq.filePath"))
  private val nasdaqUrl = new URL(config.getString("nasdaq.url"))
  private val maxFileAgeInDays = config.getInt("nasdaq.maxFileAgeInDays")

  def fetchSymbols = {

    val fileExists = nasdaqPath.toFile.exists
    lazy val fileLastModified = Files.getLastModifiedTime(nasdaqPath).toInstant
    lazy val fileAgeInDays = java.time.Duration.between(fileLastModified, Clock.systemDefaultZone.instant).toDays

    val fileContents = if (!fileExists || fileAgeInDays > maxFileAgeInDays) {
      val lines = Source.fromURL(nasdaqUrl).getLines.toSeq
      Files.write(nasdaqPath, lines.mkString("\n").getBytes(StandardCharsets.UTF_8))
      logger.info(s"Fetched Nasdaq data from $nasdaqUrl")
      lines
    } else Files.readAllLines(nasdaqPath, StandardCharsets.UTF_8).asScala

    val stocksData = fileContents.drop(1).dropRight(1).map(_.split('|')).collect {
      case a if (a.size == 8) =>
        val (place, symbol, name, category, etf) = ("NASDAQ", a(0), a(1).split('-').head.trim, a(2), a(6) == "Y")
        Stock(s"$place:$symbol", name, place, symbol, category, etf)
    }

    val db = Database.forConfig("h2file1")

    try {
      val stocks = TableQuery[Stocks]

      val deleteStocksAction = db.run(stocks.delete).map(numDeletedRows => logger.debug(s"Deleted $numDeletedRows Stocks"))
      Await.result(deleteStocksAction, 1.seconds)

      val loadStocksAction = db.run(stocks ++= stocksData).map(a => logger.info(s"Loaded ${a.get} Stocks"))
      Await.result(loadStocksAction, Duration.Inf)

    } finally db.close

  }
}

object FetchSymbols extends App {
  new NasdaqInterface().fetchSymbols
}