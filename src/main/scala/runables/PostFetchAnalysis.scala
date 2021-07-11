package runables

import java.util.concurrent.{Callable, Executors, TimeUnit}

import helper.MathHelper._
import helper.ProgressHelper
import persistence.PostgresProfile.api._
import persistence.Tables.Quotes
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode

object PostFetchAnalysis extends App {
  val db = Database.forConfig("db")

  val quotes = TableQuery[Quotes]

  val stocksQuery = quotes.groupBy(_.stockId).map(_._1) //.filter(_ === "AAPL")

  val stocks = Await.result(db.run(stocksQuery.result), Duration.Inf)

  val progressBar = new ProgressHelper(stocks.size)

  protected final val executorService = Executors.newFixedThreadPool(30)

  val takeLast = 50

  val z = stocks.map { stockId =>
    new Callable[(String, Seq[Quotes#TableElementType])] {
      override def call: (String, Seq[Quotes#TableElementType]) = {
        //9h x 4 (15min) = 36 quotes/d * 14d = 504 quotes/ stock in 14d
        //For 504 without threading takes ~50secs, with threading ~20 in test environment
        val query = quotes.filter(_.stockId === stockId).sortBy(_.lastTrade.desc).take(takeLast)

        progressBar += 1

        stockId -> Await.result(db.run(query.result), Duration.Inf)

      }
    }
  }


  val e = z.map(a => executorService.submit(a)).map(_.get()).toMap

  val finished = executorService.awaitTermination(1, TimeUnit.SECONDS)

  executorService.shutdown

  if (finished) db.close

  progressBar.finish
  //case class Quote(stockId: String, lastTrade: ZonedDateTime, price: BigDecimal, avgPrice50: BigDecimal, volume: Long, avgVolume: Long)

  val a = e.map { case (u, aa) => u -> aa.map(_.price).reverse.weightedTrend(takeLast) }.toSeq.sortBy(_._2).takeRight(30).reverse

  val mean = a.map(_._2).mean

  val oo = a.filter(_._2 / mean > 1.1).map { case (a, b) => f"$a%8s ${b.setScale(5, RoundingMode.HALF_UP)}  -> " + e(a).take(10).map(_.price).reverse.mkString(", ") }
  println(oo.mkString("\n"))

println("-" *100)

  val a2 = e.map { case (u, aa) => u -> aa.map(_.volume).reverse.weightedTrend(takeLast) }.toSeq.sortBy(_._2).takeRight(30).reverse
  val mean2 = a2.map(_._2).mean
  val oo2 = a2.filter(_._2 / mean2 > 1.1).map { case (a, b) => f"$a%8s ${b.setScale(5, RoundingMode.HALF_UP)}  -> " + e(a).take(10).map(_.volume).reverse.mkString(", ") }
  println(oo2.mkString("\n"))
  println("-" *100)
}
