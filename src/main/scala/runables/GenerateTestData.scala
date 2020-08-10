package runables

import java.time.ZonedDateTime
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.{yahooFetchIntervalInMinutes, zoneIdOf}
import helper.MathHelper._
import helper.ProgressBar
import interfaces.{NasdaqInterface, YahooInterface}
import persistence.Classes.Exchange
import persistence.PostgresProfile.api._
import persistence.Tables.Quotes

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode

object GenerateTestData extends App with LazyLogging {

  InitDatabase.init

  val exchange = Exchange("nasdaq")

  val nasdaqInterface = new NasdaqInterface
  nasdaqInterface.fetchStocks

  val yahooInterface = new YahooInterface
  yahooInterface.fetchQuotes(exchange)

  val simulatedFetchCount = 1000

  //--------------------------------------------------------

  val db = Database.forConfig("db")

  val quotes = TableQuery[Quotes]
  val allQuotes = Await.result(db.run(quotes.result), Duration.Inf)

  logger.info(s"Generating Quotes...")

  val totalCount = simulatedFetchCount * allQuotes.map(_.stockId).toSet.size
  val progressBar = new ProgressBar(totalCount)
  progressBar.showSpeed = true

  val now = ZonedDateTime.now(zoneIdOf(exchange))

  // In the test environment the difference between sequential and threaded is:
  // 8,600/s sequentially generated, ~40,000/s when generated with threads
  // CPU load is ~60% when sequential, 100% when threaded
  val runnables = allQuotes.groupBy(_.stockId).map { case (_, b) =>
    new Runnable {
      override def run = {

        var allQuotesPerStock = b.sortBy(_.lastTrade.toEpochSecond).takeRight(20)

        Range(0, simulatedFetchCount).foreach { i =>

          val last = allQuotesPerStock.head

          val newQuote = last.copy(
            lastTrade = now.plusMinutes(yahooFetchIntervalInMinutes.length * i),
            price = allQuotesPerStock.map(_.price).generateNext(0.01).setScale(2, RoundingMode.HALF_UP)
          )

          allQuotesPerStock = Seq(newQuote).++:(allQuotesPerStock)
        }

        progressBar += Await.result(db.run(quotes.insertOrUpdateAll(allQuotesPerStock)), Duration.Inf).getOrElse(0)
      }
    }
  }

  val executorService = Executors.newFixedThreadPool(10)
  runnables.foreach(executorService.execute)

  val finished = executorService.awaitTermination(1, TimeUnit.SECONDS)

  executorService.shutdown

  if (finished) db.close

}