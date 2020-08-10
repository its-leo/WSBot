package helper

import scala.util.Random

object MathHelper {

  def getRandomIntBetween(one: Int, another: Int): Int = (another + Random.nextInt(one))

  //https://www.javamex.com/tutorials/random_numbers/gaussian_distribution_2.shtml
  def getRandomGaussian(range: Double, mean: BigDecimal): BigDecimal = Random.nextGaussian * range + mean


  implicit class agdBigDecimal(values: Seq[BigDecimal]) {
    def mean: BigDecimal = values.sum / values.length

    def trend: BigDecimal = values.sliding(2).map(a => a.last - a.head).toSeq.mean

    def weightedTrend: BigDecimal = values.takeRight(10).sliding(2).zipWithIndex.map { case (a, b) => (a.last - a.head) * math.sqrt(b) / a.last }.toSeq.mean

    def generateNext(sensibility: Double, rangeByLast: Int = 3, meanByLast: Int = 5): BigDecimal = {
      val trend = weightedTrend
      val range = values.takeRight(rangeByLast).mean.toDouble * sensibility
      val mean = values.takeRight(meanByLast).mean

      (getRandomGaussian(range, mean) * (trend + 1))
    }

  }
}
