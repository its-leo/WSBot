package helper

import com.typesafe.scalalogging.LazyLogging
import persistence.Tables.Stock
import yahoofinance.YahooFinance

import scala.collection.JavaConverters.mapAsScalaMapConverter

object YahooHelper extends LazyLogging {

  private val fetchLimit = 500

  def filterKnown(instruments: Seq[Stock]): Seq[Stock] = {

    val idsGrouped = instruments.map(_.id).grouped(fetchLimit)

    val knownIds = idsGrouped.flatMap { idsGroup =>
      logger.debug(idsGroup.toString())
      YahooFinance.get(idsGroup.toArray).asScala.keySet
    }.toSet

    instruments.filter(a => knownIds.contains(a.id))

  }

}
