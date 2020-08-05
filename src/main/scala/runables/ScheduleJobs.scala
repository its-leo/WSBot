package runables

import java.time._
import java.time.format.DateTimeFormatter
import java.util.TimeZone

import akka.actor.ActorSystem
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import interfaces.actors.YahooInterfaceActor
import interfaces.actors.YahooInterfaceActor.Param
import interfaces.{NasdaqInterface, YahooInterface}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ScheduleJobs extends App with LazyLogging {

  val system = ActorSystem("ScheduleJobs")
  val quartz = QuartzSchedulerExtension(system)

  //----------------------------------------------------
  //Cron configured schedule, see application.conf

  val yahooInterface = new YahooInterface
  val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")

  val timezone = TimeZone.getTimeZone(config.getString("nasdaq.timezone"))
  val yahooFetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")
  val tradingHours = config.getString("nasdaq.tradingHours")
  val tradingDays = config.getString("nasdaq.tradingDays")

  //Very ugly fix for daylight savings time. QuartzSchedulerExtension ignores it...
  val dstAdapted = tradingHours.split("-").map(a => if (timezone.useDaylightTime) a.toInt - 1 else a).mkString("-")

  val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss VV")

  val timeToleranceInSeconds = config.getInt("timeToleranceInSeconds")
  val nowTime = Clock.systemUTC.instant.atZone(timezone.toZoneId)
  logger.info("Current Nasdaq Time: " + dateFormat.format(nowTime))

  val expression = s"""${timeToleranceInSeconds} */$yahooFetchInterval $dstAdapted ? * $tradingDays"""

  val startDate = quartz.rescheduleJob(
    name = "nasdaqFromYahoo",
    receiver = yahooInterfaceActor,
    msg = Param(yahooInterface, "nasdaq"),
    description = Some("Fetch nasdaq data from yahoo"),
    cronExpression = expression,
    calendar = None,
    timezone = timezone)

  val startDateLocal = ZonedDateTime.ofInstant(startDate.toInstant, ZoneId.systemDefault)
  logger.info("Scheduler waits until " + dateFormat.format(startDateLocal))

  //----------------------------------------------------
  //Simple schedule using a runnable

  implicit val ec = ExecutionContext.global

  val nasdaqFetchInterval = config.getInt("nasdaq.fetchIntervalInDays")
  val nasdaqInterface = new NasdaqInterface
  val nasdaqRunnable = new Runnable {
    override def run(): Unit = nasdaqInterface.fetchSymbols
  }

  val nasdaqSchedule = system.scheduler.schedule(0 seconds, nasdaqFetchInterval.days)(nasdaqRunnable.run)

}