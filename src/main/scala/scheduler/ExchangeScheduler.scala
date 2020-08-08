package scheduler

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone

import akka.actor.ActorSystem
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor
import interfaces.actors.YahooInterfaceActor.Param
import interfaces.traits.ExchangeInterface

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, DurationLong}

class ExchangeScheduler extends LazyLogging {

  def scheduleFor(exchangeInterface: ExchangeInterface) {

    val exchangeName = exchangeInterface.exchangeName

    val system = ActorSystem(s"${exchangeName}_scheduler")
    val quartz = QuartzSchedulerExtension(system)

    //----------------------------------------------------
    //Cron configured schedule, see application.conf

    val yahooInterface = new YahooInterface
    val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")

    val timezone = TimeZone.getTimeZone(config.getString(s"$exchangeName.timezone"))
    val yahooFetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")
    val tradingHours = config.getString(s"$exchangeName.tradingHours")
    val tradingDays = config.getString(s"$exchangeName.tradingDays")

    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss VV")

    val fetchOffsetSeconds = config.getInt("fetchOffsetSeconds")
    val nowTime = Clock.systemUTC.instant.atZone(timezone.toZoneId)
    logger.info(s"Current $exchangeName time: " + dateFormat.format(nowTime))

    val expression = s"""${fetchOffsetSeconds} */$yahooFetchInterval $tradingHours ? * $tradingDays"""

    val startDate = quartz.rescheduleJob(
      name = s"${exchangeName}FromYahoo",
      receiver = yahooInterfaceActor,
      msg = Param(yahooInterface, exchangeName),
      description = Some(s"Fetch $exchangeName data from yahoo"),
      cronExpression = expression,
      calendar = None,
      timezone = timezone)


    val startDateLocal = ZonedDateTime.ofInstant(startDate.toInstant, ZoneId.systemDefault)
    logger.info(s"Scheduler for $exchangeName waits until " + dateFormat.format(startDateLocal))

    //----------------------------------------------------

    implicit val ec = ExecutionContext.global

    val exchangeFetchIntervalInDays = config.getInt(s"$exchangeName.fetchIntervalInDays").days

    val waitingInSeconds = (ChronoUnit.SECONDS.between(nowTime, startDateLocal) - fetchOffsetSeconds).seconds

    val exchangeDataRunnable = new Runnable {
      override def run = exchangeInterface.fetchStocks
    }

    system.scheduler.schedule(waitingInSeconds, exchangeFetchIntervalInDays)(exchangeDataRunnable.run)

  }
}