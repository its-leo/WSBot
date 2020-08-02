package runables

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

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

  //----------------------------------------------------
  //Cron configured schedule, see application.conf

  val yahooFetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")

  val time = LocalDateTime.now
  val nextStartMinutes = (yahooFetchInterval * (time.getMinute / yahooFetchInterval) + yahooFetchInterval)
  val startTime = time.truncatedTo(ChronoUnit.HOURS).plusMinutes(nextStartMinutes).atZone(ZoneId.systemDefault)

  val yahooInterface = new YahooInterface
  val yahooInterfaceActor = system.actorOf(YahooInterfaceActor.props, "fetch")
  QuartzSchedulerExtension(system).schedule("nasdaqFromYahoo", yahooInterfaceActor, Param(yahooInterface), Option(Date.from(startTime.toInstant)))

  //----------------------------------------------------
  //Simple schedule using a runnable

  implicit val ec = ExecutionContext.global

  val nasdaqFetchInterval = config.getInt("nasdaq.fetchIntervalInDays")
  val nasdaqInterface = new NasdaqInterface
  val nasdaqRunnable = new Runnable {
    override def run(): Unit = nasdaqInterface.fetchSymbols
  }

  val nasdaqSchedule = system.scheduler.schedule(0 seconds, nasdaqFetchInterval.days)(nasdaqRunnable.run)

  //----------------------------------------------------
  logger.info("Started schedule jobs")
}