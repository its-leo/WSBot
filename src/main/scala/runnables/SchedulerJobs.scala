package runnables

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import akka.actor.ActorSystem
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.LazyLogging
import helper.ConfigHelper.config
import interfaces.actors.YahooInterfaceActor
import interfaces.actors.YahooInterfaceActor.Param

object SchedulerJobs extends App with LazyLogging {

  val fetchInterval = config.getInt("yahoo.fetchIntervalInMinutes")

  val system = ActorSystem("Runnables.JobSchedulerCron")

  val time = LocalDateTime.now
  val nextStart = time.truncatedTo(ChronoUnit.HOURS).plusMinutes(fetchInterval * (time.getMinute / fetchInterval) + fetchInterval).atZone(ZoneId.systemDefault)

  logger.info(s"Schedule starts at $nextStart")

  val fetchActor = system.actorOf(YahooInterfaceActor.props, "fetch")
  QuartzSchedulerExtension(system).schedule("nasdaqFromYahoo", fetchActor, Param(), Option(Date.from(nextStart.toInstant)))

}