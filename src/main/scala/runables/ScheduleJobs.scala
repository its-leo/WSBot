package runables

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDateTime, ZoneId}
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


  val tradingDays = config.getString("nasdaq.tradingDays")
  val tradingHours = config.getString("nasdaq.tradingHours")
  val timeToleranceInSeconds = config.getInt("timeToleranceInSeconds")
  val timezone = TimeZone.getTimeZone(config.getString("nasdaq.timezone"))
  val expression = s"""${timeToleranceInSeconds} */$yahooFetchInterval $tradingHours ? * $tradingDays"""

  //quartz.wait(waitingMillis)
  quartz.rescheduleJob(
    name = "nasdaqFromYahoo",
    receiver = yahooInterfaceActor,
    msg = Param(yahooInterface, "nasdaq"),
    description = Some("Fetch nasdaq data from yahoo"),
    cronExpression = expression,
    calendar = None,
    timezone = timezone)

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