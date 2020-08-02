package helper

import com.typesafe.config.{Config, ConfigFactory}

object ConfigHelper {
  val config: Config = ConfigFactory.load
}
