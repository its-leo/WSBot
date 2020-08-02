package interfaces.actors

import akka.actor.{Actor, Props}
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor.Param

object YahooInterfaceActor {
  case class Param(interface: YahooInterface)
  def props: Props = Props[YahooInterfaceActor]
}

class YahooInterfaceActor extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case Param(interface) => interface.fetchQuotes
  }
}