package interfaces

import interfaces.traits.ExchangeInterface
import persistence.Tables.Stock

class XetraInterface extends ExchangeInterface {
  override val exchangeName: String = "xetra"

  override def mapLinesToStocks(lines: Seq[String]): Seq[Stock] = lines.drop(4).map(_.split(';')).collect {
    case a if (a(18) == "CS" || a(18) == "ETF") && !a(7).isEmpty => //CS == Stock, ETF == ...
      val region = a(3).splitAt(2)._1 //DE, AT etc...
      val symbol = a(7)
      val id = region match {
        case "US" => symbol
        case _ => s"$symbol.$region"
      }
      val name = a(2).replace("O.N.", "").trim
      val etf = a(18) == "ETF"
      Stock(id, name, exchangeName.toUpperCase, symbol, etf)
  }
}

object XetraInterface extends App {
  val a = new XetraInterface
  a.fetchStocks
}