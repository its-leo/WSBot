package runables

import com.typesafe.scalalogging.LazyLogging
import interfaces._
import scheduler.ExchangeScheduler

object ScheduleJobs extends App with LazyLogging {

  val exchangeScheduler = new ExchangeScheduler

  val nasdaqInterface = new NasdaqInterface
  exchangeScheduler.scheduleFor(nasdaqInterface)

  println("-" * 100)

  val xetraInterface = new XetraInterface
  exchangeScheduler.scheduleFor(xetraInterface)

}