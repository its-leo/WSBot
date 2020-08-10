package persistence

import java.time.ZonedDateTime

import persistence.PostgresProfile.api._
import slick.lifted.ForeignKeyQuery

object Classes {

 case class Exchange(name:String) extends AnyVal
}

object Tables {

  //------------------------------------------------
  case class Quote(stockId: String, lastTrade: ZonedDateTime, price: BigDecimal, avgPrice50: BigDecimal, volume: Long, avgVolume: Long)

  class Quotes(tag: Tag) extends Table[Quote](tag, "Quotes") {

    def pk = primaryKey("pk", (stockId, lastTrade))

    def stockId = column[String]("stockId")

    def lastTrade = column[ZonedDateTime]("lastTrade")

    def price = column[BigDecimal]("price")

    def avgPrice50 = column[BigDecimal]("avgPrice50")

    def volume = column[Long]("volume")

    def avgVolume = column[Long]("avgVolume")

    override def * = (stockId, lastTrade, price, avgPrice50, volume, avgVolume) <> ((Quote.apply _).tupled, Quote.unapply)

    //def uniqueQuote = index("unique_quote", (stockId, lastTrade), unique = true)


    //A reified foreign key relation that can be navigated to create a join
    def stock: ForeignKeyQuery[Stocks, Stock] = foreignKey("stock_fk", stockId, TableQuery[Stocks])(_.id)

  }

  //------------------------------------------------

  case class Stock(id: String, name: String, exchange: String, symbol: String, etf: Boolean)

  class Stocks(tag: Tag) extends Table[Stock](tag, "Stocks") {

    def id = column[String]("id", O.PrimaryKey)

    def name = column[String]("name")

    def exchange = column[String]("exchange")

    def symbol = column[String]("symbol")

    def etf = column[Boolean]("etf")

    def * = (id, name, exchange, symbol, etf) <> (Stock.tupled, Stock.unapply)

  }

}