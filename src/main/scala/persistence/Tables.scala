package persistence

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import slick.lifted.ForeignKeyQuery

//------------------------------------------------
case class Quote(id: Option[Long], stockId: String, lastTrade: Timestamp, price: BigDecimal, avgPrice50: BigDecimal, volume: Long, avgVolume: Long)

class Quotes(tag: Tag) extends Table[Quote](tag, "Quotes") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def stockId = column[String]("stockId")

  def lastTrade = column[Timestamp]("lastTrade")

  def price = column[BigDecimal]("price")

  def avgPrice50 = column[BigDecimal]("avgPrice50")

  def volume = column[Long]("volume")

  def avgVolume = column[Long]("avgVolume")

  def * = (id.?, stockId, lastTrade, price, avgPrice50, volume, avgVolume) <> (Quote.tupled, Quote.unapply)

  //A reified foreign key relation that can be navigated to create a join
  def stock: ForeignKeyQuery[Stocks, Stock] = foreignKey("stock_fk", stockId, TableQuery[Stocks])(_.id)

}

//------------------------------------------------

case class Stock(id: String, name: String, place: String, symbol: String, category: String, etf: Boolean)

class Stocks(tag: Tag) extends Table[Stock](tag, "Stocks") {

  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def place = column[String]("place")

  def symbol = column[String]("symbol")

  def category = column[String]("category")

  def etf = column[Boolean]("etf")

  def * = (id, name, place, symbol, category, etf) <> (Stock.tupled, Stock.unapply)

}