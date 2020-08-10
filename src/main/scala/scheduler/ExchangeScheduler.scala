package scheduler

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper._
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor
import interfaces.actors.YahooInterfaceActor.Param
import interfaces.traits.ExchangeInterface

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ExchangeScheduler extends LazyLogging {

  def scheduleFor(exchangeInterface: ExchangeInterface) {

    val exchange = exchangeInterface.exchange

    val system = ActorSystem(s"${exchange.name}_scheduler")
    val quartz = QuartzSchedulerExtension(system)

    //----------------------------------------------------
    //Cron configured schedule, see application.conf

    val yahooInterface = new YahooInterface
    val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")

    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss VV")


    val nowTime = Clock.systemUTC.instant.atZone(timezoneOf(exchange).toZoneId)
    logger.info(s"Current ${exchange.name} time: " + dateFormat.format(nowTime))

    val expression = s"""${fetchOffsetSeconds.length} */${yahooFetchIntervalInMinutes.length} ${tradingHoursOf(exchange)} ? * ${tradingDaysOf(exchange)}"""

    val startDate = quartz.rescheduleJob(
      name = s"${exchange.name}FromYahoo",
      receiver = yahooInterfaceActor,
      msg = Param(yahooInterface, exchange),
      description = Some(s"Fetch ${exchange.name} data from yahoo"),
      cronExpression = expression,
      calendar = None,
      timezone = timezoneOf(exchange))

    val startDateLocal = ZonedDateTime.ofInstant(startDate.toInstant, ZoneId.systemDefault)
    logger.info(s"Scheduler for ${exchange.name} waits until " + dateFormat.format(startDateLocal))

    //----------------------------------------------------

    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    val waitingInSeconds = (ChronoUnit.SECONDS.between(nowTime, startDateLocal) - fetchOffsetSeconds.length).seconds

    val exchangeDataRunnable = new Runnable {
      override def run(): Unit = exchangeInterface.fetchStocks
    }

    system.scheduler.schedule(waitingInSeconds, fetchIntervalInDaysFor(exchange))(exchangeDataRunnable.run())

  }
}