package runables

import com.typesafe.scalalogging.LazyLogging
import interfaces._
import scheduler.ExchangeScheduler

object ScheduleJobs extends App with LazyLogging {


  val nasdaqInterface = new NasdaqInterface
  val nasdaqScheduler = new ExchangeScheduler(nasdaqInterface)
  nasdaqScheduler.schedule

  println("-" * 100)


  val xetraInterface = new XetraInterface
  val xetraScheduler = new ExchangeScheduler(xetraInterface)
  xetraScheduler.schedule

  println("-" * 100)

}