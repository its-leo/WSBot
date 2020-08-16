package runables

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, LocalDate, ZonedDateTime}
import java.util.Locale.forLanguageTag
import java.util.concurrent.{Callable, Executors, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.{fetchIntervalInMinutes, tradingDaysOf, tradingHoursOf, zoneIdOf}
import helper.MathHelper._
import helper.ProgressBar
import interfaces.{NasdaqInterface, YahooInterface}
import persistence.Classes.Exchange
import persistence.PostgresProfile.api._
import persistence.Tables.{Quote, Quotes}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.math.BigDecimal.RoundingMode

object GenerateTestData extends App with LazyLogging {

  val endDate = LocalDate.of(2020, 9, 18)

  //-----------------------------------------------------------------------------

  InitDatabase.init

  val exchange = Exchange("nasdaq")
  val zoneId = zoneIdOf(exchange)

  val nasdaqInterface = new NasdaqInterface
  nasdaqInterface.fetchStocks

  val yahooInterface = new YahooInterface
  yahooInterface.fetchQuotes(exchange)

  //-----------------------------------------------------------------------------

  val weekdays = tradingDaysOf(exchange).split('-').take(2).map { a =>
    val formatter = DateTimeFormatter.ofPattern("E", forLanguageTag("en"))
    DayOfWeek.from(formatter.parse(a.toLowerCase.capitalize))
  }

  val (startWeekday, endWeekday) = (weekdays.head.getValue, weekdays.last.getValue)

  val tradingHours = tradingHoursOf(exchange).split('-').take(2).map(_.toInt)
  val (startHour, endHour) = (tradingHours.head, tradingHours.last)


  val lastTime = endDate.atTime(endHour, 0)

  val dates = LocalDate.now(zoneId).datesUntil(endDate.plusDays(1)).iterator.asScala.toSeq.filter { a =>
    val weekday = a.getDayOfWeek.getValue
    (weekday <= endWeekday && weekday >= startWeekday)
  }

  val startDate = dates.head
  val firstTime = startDate.atTime(startHour, 0)

  val fetchesPerDate = (ChronoUnit.MINUTES.between(firstTime, startDate.atTime(endHour, 0)) / fetchIntervalInMinutes).toInt + 1

  //-----------------------------------------------------------------------------

  val db = Database.forConfig("db")

  val quotes = TableQuery[Quotes]
  val allQuotes = Await.result(db.run(quotes.result), Duration.Inf)

  logger.info(s"Generating Quotes...")

  val totalCount = dates.size * fetchesPerDate * allQuotes.map(_.stockId).toSet.size
  val progressBar = new ProgressBar(totalCount)
  progressBar.showSpeed = true

  val now = ZonedDateTime.now(zoneId)

  // In the test environment the difference between sequential and threaded is:
  // 8,600/s sequentially generated, ~60,000/s when generated with threads
  // CPU load is ~60% when sequential, 100% when threaded
  val z = allQuotes.groupBy(_.stockId).map { case (_, b) =>
    new Callable[Seq[Quote]] {
      override def call(): Seq[Quote] = {

        val allQuotesPerStock = b.sortBy(_.lastTrade.toEpochSecond).takeRight(20)
        val last = allQuotesPerStock.head

        val newQuotes = dates.flatMap { date =>

          val newVolume = allQuotesPerStock.map(_.volume).generateNext(0.01).toLong

          Range(0, fetchesPerDate).map { i =>

            val time = date.atTime(startHour, 0).plusMinutes(i * fetchIntervalInMinutes)
            last.copy(
              lastTrade = time.atZone(zoneId),
              volume = newVolume,
              price = if (lastTime.equals(time)) {
                allQuotesPerStock.map(_.price).generateNext(0.01).setScale(2, RoundingMode.HALF_UP) * 1.3
              } else allQuotesPerStock.map(_.price).generateNext(0.01).setScale(2, RoundingMode.HALF_UP)
            )

          }

        }

        progressBar += Await.result(db.run(quotes.insertOrUpdateAll(newQuotes)), Duration.Inf).getOrElse(0)

        newQuotes
      }
    }
  }

  val executorService = Executors.newFixedThreadPool(4)

  z.map(a => executorService.submit(a)).foreach(_.get)

  val finished = executorService.awaitTermination(1, TimeUnit.SECONDS)

  executorService.shutdown

  if (finished) db.close

  progressBar.finish()

}