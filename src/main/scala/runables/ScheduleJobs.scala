package runables

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Clock, Duration, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Date, TimeZone}

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

  val yahooFetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")

  val now = LocalDateTime.now
  val nextStartMinutes = (yahooFetchInterval * (now.getMinute / yahooFetchInterval) + yahooFetchInterval)
  val nextStartTime = now.truncatedTo(ChronoUnit.HOURS).plusMinutes(nextStartMinutes)
  val waitingMillis = Duration.between(now, nextStartTime).toMillis
  val startDateOption = Option(Date.from(nextStartTime.atZone(ZoneId.systemDefault).toInstant))

  val yahooInterface = new YahooInterface
  val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")
  //quartz.schedule("nasdaqFromYahoo", yahooInterfaceActor, Param(yahooInterface, "nasdaq"), startDateOption)


  val timezone = TimeZone.getTimeZone(config.getString("nasdaq.timezone"))
  val tradingDays = config.getString("nasdaq.tradingDays")
  val tradingHours = config.getString("nasdaq.tradingHours")

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

  logger.info("Scheduler waits until " + dateFormat.format(ZonedDateTime.ofInstant(startDate.toInstant, ZoneId.systemDefault)))


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