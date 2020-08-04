package runables

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.LazyLogging
import persistence.Tables._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object DeleteAllData extends App with LazyLogging {

  val db = Database.forConfig("db")

  val tables = Set(TableQuery[Quotes], TableQuery[Stocks])

  try tables.foreach { table =>
    val deleteQuotesAction = db.run(table.delete).map(numDeletedRows => logger.info(s"Deleted $numDeletedRows rows"))
    Await.result(deleteQuotesAction, Duration.Inf)
  } finally db.close

}
