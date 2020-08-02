package runnables

import com.typesafe.scalalogging.LazyLogging
import persistence.{Quotes, Stocks}
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

object InitDatabase extends App with LazyLogging {

  val db = Database.forConfig("h2file1")

  try {

    val quotes = TableQuery[Quotes]
    val stocks = TableQuery[Stocks]

    val setupAction: DBIO[Unit] = DBIO.seq(
      quotes.schema.createIfNotExists,
      stocks.schema.createIfNotExists
    )

    Await.result(db.run(setupAction), Duration.Inf)

    val tables = Await.result(db.run(MTable.getTables), 1.seconds).toList
    logger.info("Tables: " + tables.map(_.name.name).mkString(", "))

  } finally db.close

}
