package persistence

import com.github.tminglei.slickpg._
import com.typesafe.config.ConfigFactory
import slick.basic.Capability

object DBComponent {

  private val config = ConfigFactory.load()

  val driver = config.getString("rdbms.properties.driver") match {
    case "org.h2.Driver" => slick.jdbc.H2Profile
    case _               => PostgresProfile
  }

  import driver.api._

  val db: Database = Database.forConfig("rdbms.properties")

}

trait PostgresProfile extends ExPostgresProfile {

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] = super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api: MyAPI.type = MyAPI

  object MyAPI extends API
}

object PostgresProfile extends PostgresProfile