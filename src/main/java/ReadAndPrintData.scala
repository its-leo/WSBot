import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ReadAndPrintData extends App {

  val db = Database.forConfig("h2file1")

  try {

    val quotes = TableQuery[Quotes]
    val f = db.run(quotes.result.map(a => println(s"${a.size} Quotes:\n${a.mkString("\n")}")))
    Await.result(f, Duration.Inf)

    println("-----------------------------")

    /*
    val stocks = TableQuery[Stocks]
    val g = db.run(stocks.result.map(a => println(s"${a.size} Stocks:\n${a.mkString("\n")}")))
    Await.result(g, Duration.Inf)
*/

  } finally db.close
}