package runables

import interfaces.Stocks
import akka.actor.TypedActor.dispatcher
import com.typesafe.scalalogging.LazyLogging
import persistence.{Quotes, Stocks}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ReadAndPrintData extends App with LazyLogging {

  val db = Database.forConfig("h2file1")

  try {

    val quotes = TableQuery[Quotes]
    //val f = db.run(quotes.result.map(a => logger.info(s"${a.size} Interfaces.Quotes:\n${a.mkString("\n")}")))
    val f = db.run(quotes.result.map(a => logger.debug(s"${a.size} Interfaces.Quotes")))
    Await.result(f, Duration.Inf)

    val stocks = TableQuery[Stocks]
    val g = db.run(stocks.result.map(a => logger.debug(s"${a.size} Interfaces.Stocks")))
    Await.result(g, Duration.Inf)

  } finally db.close
}
