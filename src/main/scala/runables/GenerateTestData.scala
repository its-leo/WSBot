package runables

import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import helper.ProgressBar
import interfaces.{NasdaqInterface, YahooInterface}
import persistence.PostgresProfile.api._
import persistence.Tables.Quotes

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode
import scala.util.Random

object GenerateTestData extends App with LazyLogging {

  InitDatabase.init

  val exchangeName = "nasdaq"

  val nasdaqInterface = new NasdaqInterface
  nasdaqInterface.fetchStocks

  val yahooInterface = new YahooInterface
  yahooInterface.fetchQuotes(exchangeName)

  val db = Database.forConfig("db")

  val yahooFetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")

  val timezone = config.getString(s"${exchangeName.toLowerCase}.timezone")

  def getRandomIntBetween(one: Int, another: Int) = (another + Random.nextInt(one))

  //https://www.javamex.com/tutorials/random_numbers/gaussian_distribution_2.shtml
  def getRandomGaussian(range: Double, mean: BigDecimal) = Random.nextGaussian * range + mean

  implicit class agdBigDecimal(values: Seq[BigDecimal]) {
    def mean = values.sum / values.length

    def trend = values.sliding(2).map(a => a.last - a.head).toSeq.mean

    def weightedTrend = values.takeRight(10).sliding(2).zipWithIndex.map { case (a, b) => (a.last - a.head) * math.sqrt(b) / a.last }.toSeq.mean

    def generateNext(sensibility: Double, rangeByLast: Int = 3, meanByLast: Int = 5) = {
      val trend = weightedTrend
      val range = values.takeRight(rangeByLast).mean.toDouble * sensibility
      val mean = values.takeRight(meanByLast).mean

      (getRandomGaussian(range, mean) * (trend + 1))
    }

  }

  val simulatedFetchCount = 1000


  val quotes = TableQuery[Quotes]
  val allQuotes = Await.result(db.run(quotes.result), Duration.Inf)


  logger.info(s"Generating Quotes...")

  val totalCount = simulatedFetchCount * allQuotes.map(_.stockId).toSet.size
  var progressBar = new ProgressBar(totalCount)
  progressBar.showSpeed = true

  val now = ZonedDateTime.now(ZoneId.of(timezone))

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
            lastTrade = now.plusMinutes(yahooFetchInterval * i),
            price = allQuotesPerStock.map(_.price).generateNext(0.01).setScale(2, RoundingMode.HALF_UP)
          )

          allQuotesPerStock = Seq(newQuote).++:(allQuotesPerStock)
        }

        progressBar += Await.result(db.run(quotes.insertOrUpdateAll(allQuotesPerStock)), Duration.Inf).getOrElse(0)
      }
    }
  }


  val es = Executors.newFixedThreadPool(10)
  runnables.foreach(es.execute)

  val finished = es.awaitTermination(1, TimeUnit.SECONDS)

  es.shutdown

  if (finished) db.close

}
