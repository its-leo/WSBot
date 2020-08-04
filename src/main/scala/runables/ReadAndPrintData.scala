package runables

import com.typesafe.scalalogging.LazyLogging
import persistence.Tables._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ReadAndPrintData extends App with LazyLogging {

  val db = Database.forConfig("db")

  try {

    val quotes = TableQuery[Quotes]
    val fu = db.run(quotes.result.map(a => logger.info(s"${a.size} Quotes:\n${a.mkString("\n")}")))
    val f = db.run(quotes.result.map(a => logger.debug(s"${a.size} Quotes")))
    Await.result(f, Duration.Inf)

    val stocks = TableQuery[Stocks]
    val g = db.run(stocks.result.map(a => logger.debug(s"${a.size} Stocks")))
    Await.result(g, Duration.Inf)

  } finally db.close
}
