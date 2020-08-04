package persistence

import java.time.ZonedDateTime

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.driver.JdbcProfile
import slick.lifted.ForeignKeyQuery

trait MyPostgresProfile extends ExPostgresProfile with PgArraySupport {

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits


  override protected def computeCapabilities: Set[Capability] = super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

}

object MyPostgresProfile extends MyPostgresProfile


import persistence.MyPostgresProfile.api._

object Tables {

  //------------------------------------------------
  case class Quote(id: Option[Long], stockId: String, lastTrade: ZonedDateTime, price: BigDecimal, avgPrice50: BigDecimal, volume: Long, avgVolume: Long)

  class Quotes(tag: Tag) extends Table[Quote](tag, "Quotes") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def stockId = column[String]("stockId")

    def lastTrade = column[ZonedDateTime]("lastTrade")

    def price = column[BigDecimal]("price")

    def avgPrice50 = column[BigDecimal]("avgPrice50")

    def volume = column[Long]("volume")

    def avgVolume = column[Long]("avgVolume")

    override def * = (id.?, stockId, lastTrade, price, avgPrice50, volume, avgVolume) <> ((Quote.apply _).tupled, Quote.unapply)

    def uniqueQuote = index("unique_quote", (stockId, lastTrade), unique = true)

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

}