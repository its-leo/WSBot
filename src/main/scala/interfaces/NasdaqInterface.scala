package interfaces

import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Clock

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import persistence.Tables._
import slick.jdbc.H2Profile.api._

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.Source

class NasdaqInterface extends LazyLogging {

  val place = "nasdaq"
  private val path = Paths.get(config.getString(s"$place.filePath"))
  private val url = new URL(config.getString(s"$place.url"))
  private val fetchIntervalInDays = config.getInt(s"$place.fetchIntervalInDays")

  def fetchSymbols = {

    val fileExists = path.toFile.exists
    lazy val fileLastModified = Files.getLastModifiedTime(path).toInstant
    lazy val fileAgeInDays = java.time.Duration.between(fileLastModified, Clock.systemDefaultZone.instant).toDays

    val fileContents = if (!fileExists || fileAgeInDays >= fetchIntervalInDays) {
      val lines = Source.fromURL(url).getLines.toSeq
      Files.write(path, lines.mkString("\n").getBytes(StandardCharsets.UTF_8))
      logger.info(s"Fetched $place data from $url")
      lines
    } else Files.readAllLines(path, StandardCharsets.UTF_8).asScala

    val stocksData = fileContents.drop(1).dropRight(1).map(_.split('|')).collect {
      case a if (a.size == 8) =>
        val (symbol, name, category, etf) = (a(0), a(1).split('-').head.trim, a(2), a(6) == "Y")
        Stock(s"${place.toUpperCase}:$symbol", name, place.toUpperCase, symbol, category, etf)
    }

    val db = Database.forConfig("db")

    try {

      val stocks = TableQuery[Stocks]

      val actions = DBIO.seq(
        stocks.delete,
        stocks ++= stocksData
      )

      Await.result(db.run(actions), 1.seconds)

    } finally db.close
  }
}