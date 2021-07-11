package scheduler

import java.time._
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper._
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor
import interfaces.actors.YahooInterfaceActor.Param
import interfaces.traits.ExchangeInterface

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

class ExchangeScheduler(exchangeInterface: ExchangeInterface) extends LazyLogging {

  private val exchange = exchangeInterface.exchange

  private val system = ActorSystem(s"${exchange.name}_scheduler")
  private val quartz = QuartzSchedulerExtension(system)

  def schedule {

    val exchangeDataRunnable = new Runnable {
      override def run: Unit = exchangeInterface.fetchStocks
    }

    system.scheduler.schedule(0.seconds, fetchIntervalInDaysFor(exchange).days, exchangeDataRunnable)

    //----------------------------------------------------
    //Cron configured schedule, see application.conf

    val yahooInterface = new YahooInterface
    val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")

    val th = tradingHoursOf(exchange).split("-").map(_.toInt)
    val expression = s"""0 */$fetchIntervalInMinutes ${th.head}-${th.last-1} ? * ${tradingDaysOf(exchange)}"""



    val startDate = quartz.rescheduleJob(
      name = s"${exchange.name}FromYahoo",
      receiver = yahooInterfaceActor,
      msg = Param(yahooInterface, exchange),
      description = Some(s"Fetch ${exchange.name} data from yahoo"),
      cronExpression = expression,
      calendar = None,
      timezone = timezoneOf(exchange))

    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss VV")

    val nowTime = Clock.systemUTC.instant.atZone(timezoneOf(exchange).toZoneId)
    logger.info(s"Current ${exchange.name} time: " + dateFormat.format(nowTime))

    val startDateLocal = ZonedDateTime.ofInstant(startDate.toInstant, ZoneId.systemDefault)
    logger.info(s"Scheduler for ${exchange.name} waits until " + dateFormat.format(startDateLocal))
  }
}