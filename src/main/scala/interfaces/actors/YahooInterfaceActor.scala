package interfaces.actors

import akka.actor.{Actor, Props}
import interfaces.YahooInterface
import interfaces.actors.YahooInterfaceActor.Param
import persistence.Classes.Exchange

object YahooInterfaceActor {
  case class Param(interface: YahooInterface, exchange: Exchange)
  def props: Props = Props[YahooInterfaceActor]
}

class YahooInterfaceActor extends Actor {
  override def receive: PartialFunction[Any, Unit] = {
    case Param(interface, exchange) => interface.fetchQuotes(exchange)
  }
}