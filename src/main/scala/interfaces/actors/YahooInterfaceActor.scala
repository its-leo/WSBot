package interfaces.actors

import akka.actor.{Actor, Props}
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor.Param

object YahooInterfaceActor {
  case class Param(interface: YahooInterface, place: String)
  def props: Props = Props[YahooInterfaceActor]
}

class YahooInterfaceActor extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case Param(interface, place) => interface.fetchQuotes(place)
  }
}