import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.typesafe.config.ConfigFactory
import slick.jdbc.H2Profile.api._

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Source

object LoadNasdaqData extends App {

  val fileContents = ConfigFactory.load.getString("nasdaqpath") match {
    case a if (a.startsWith("http")) => Source.fromURL(new URL(a)).getLines.toSeq
    case b => Files.readAllLines(Paths.get(b), StandardCharsets.UTF_8).asScala
  }

  val stocksData = fileContents.drop(1).dropRight(1).map(_.split('|')).collect { case a if (a.size == 8) =>
    val (place, symbol, name, category, etf) = ("NASDAQ", a(0), a(1).split('-').head.trim, a(2), a(6) == "Y")
    Stock(s"$place:$symbol", name, place, symbol, category, etf)
  }.toSeq

  val db = Database.forConfig("h2file1")
  try {
    val stocks = TableQuery[Stocks]

    val deleteStocksAction = db.run(stocks.delete).map(numDeletedRows => println(s"Deleted $numDeletedRows Stocks"))
    Await.result(deleteStocksAction, 1.seconds)

    val loadStocksAction = db.run(stocks ++= stocksData).map(a => println(s"Loaded ${a.get} Stocks"))
    Await.result(loadStocksAction, Duration.Inf)

  } finally db.close
}