package interfaces.actors

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor.Param

object YahooInterfaceActor {

  case class Param(message: String = "")

  def props: Props = Props[YahooInterfaceActor]
}

class YahooInterfaceActor extends Actor with LazyLogging {
  override def receive: PartialFunction[Any, Unit] = {
    case Param(message) => {
      if (!message.isEmpty) logger.info(message)
      val interface = new YahooInterface
      interface.fetchQuotes
    }
  }
}