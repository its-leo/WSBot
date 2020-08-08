package interfaces

import interfaces.traits.ExchangeInterface
import persistence.Tables.Stock

class NasdaqInterface extends ExchangeInterface {
  override val exchangeName: String = "nasdaq"

  override def mapLinesToStocks(lines: Seq[String]): Seq[Stock] = lines.drop(4).map(_.split('|')).collect {
    case a if a.length == 8 => //No ETF and Line should have 8 columns
      val (symbol, name) = (a(0), a(1).split('-').head.trim)
      Stock(symbol, name, exchangeName.toUpperCase, symbol, a(6) == "Y")
  }

}

object NasdaqInterface extends App {
  //DeleteAllData.deleteAllData
  val a = new NasdaqInterface
  a.fetchStocks
}