package helper

import java.net.URL
import java.nio.file.{Path, Paths}
import java.time.ZoneId
import java.util.TimeZone

import com.typesafe.config.{Config, ConfigFactory}
import persistence.Classes.Exchange

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ConfigHelper {
  val config: Config = ConfigFactory.load

  val fetchLimit: Int = config.getInt("yahoo.fetchLimit")

  val yahooFetchIntervalInMinutes: FiniteDuration = config.getInt("yahoo.fetchIntervalInMinutes").minutes

  val fetchOffsetSeconds: FiniteDuration = config.getInt("fetchOffsetSeconds").seconds

  def zoneIdOf(exchange: Exchange): ZoneId = ZoneId.of(config.getString(s"${exchange.name.toLowerCase}.timezone"))

  def timezoneOf(exchange: Exchange): TimeZone = TimeZone.getTimeZone(config.getString(s"${exchange.name}.timezone"))

  def tradingHoursOf(exchange: Exchange): String = config.getString(s"${exchange.name}.tradingHours")

  def tradingDaysOf(exchange: Exchange): String = config.getString(s"${exchange.name}.tradingDays")

  def fetchIntervalInDaysFor(exchange: Exchange): FiniteDuration = config.getInt(s"${exchange.name}.fetchIntervalInDays").days

  def stockUrlFor(exchange: Exchange) = new URL(config.getString(s"${exchange.name}.url"))

  def filePathFor(exchange: Exchange): Path = Paths.get(config.getString("dataPath") + s"/${exchange.name}.txt")


}
