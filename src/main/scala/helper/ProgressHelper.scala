package helper

import com.github.nscala_time.time.Imports._
import jline.TerminalFactory

/**
 * Credits to:
 * https://github.com/a8m/pb-scala
 */


/** By calling new ProgressBar with Int as a total, you'll
 * create a new ProgressBar with default configuration.
 */
class ProgressHelper(val total: Int) {
  var current = 0

  def now = DateTime.now

  final val startTime = now

  var isFinish = false
  var showSpeed, showPercent, showCounter, showTimeLeft = true

  /** Add value using += operator
   *
   * @param i the number to add to current value
   * @return current value
   */
  def +=(i: Int): Int = {
    current += i
    if (current <= total) draw
    current
  }

  private def draw {
    val width = TerminalFactory.get().getWidth()
    var prefix, base, suffix = ""
    // percent box
    if (showPercent) {
      var percent = current.toFloat / (total.toFloat / 100)
      suffix += " %.2f %% ".format(percent)
    }
    // speed box
    if (showSpeed) {
      val fromStart = (startTime to now).millis.toFloat
      val speed = current / (fromStart / 1.seconds.millis)
      suffix += "%.0f/s ".format(speed)
    }
    // time left box
    if (showTimeLeft) {
      val fromStart = (startTime to now).millis.toFloat
      val left = (fromStart / current) * (total - current)
      val dur = Duration.millis(Math.ceil(left).toLong)
      if (dur.seconds > 0) {
        if (dur.seconds < 1.minutes.seconds) suffix += "%ds".format(dur.seconds)
        else suffix += "%dm".format(dur.minutes)
      }
    }
    // counter box
    if (showCounter) {
      prefix += "%d / %d ".format(current, total)
    }

    var out = prefix + base + suffix
    if (out.length < width) {
      out += " " * (width - out.length)
    }

    Console.print("\r" + out)
  }

  /** Calling finish manually will set current to total and draw
   * the last time
   */
  def finish {
    isFinish = true
    if (current < total) +=(total - current)
    val s = (startTime to now).toDurationMillis
    val millis = s % 1000
    val second = (s / 1000) % 60
    val minute = (s / (1000 * 60)) % 60
    val hour = (s / (1000 * 60 * 60)) % 24
    println(s" | Total: %d:%02d:%02d:%09d".format(hour, minute, second, millis))
  }
}