package runables

import com.typesafe.scalalogging.LazyLogging
import persistence.PostgresProfile.api._
import persistence.Tables.{Quotes, Stocks}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object InitDatabase extends App with LazyLogging {

  val db = Database.forConfig("db")

  try {
    val quotes = TableQuery[Quotes]
    val stocks = TableQuery[Stocks]

    val initAction = DBIO.seq(
      quotes.schema.dropIfExists,
      stocks.schema.dropIfExists,
      stocks.schema.createIfNotExists,
      quotes.schema.createIfNotExists
    )

    Await.result(db.run(initAction), Duration.Inf)

  } finally db.close

}
