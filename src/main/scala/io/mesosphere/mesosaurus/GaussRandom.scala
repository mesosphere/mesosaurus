package io.mesosphere.mesosaurus

import scala.util._

/**
  * Deliver random numbers that are Gauss/normal-distributed
  * around a given mean value ("my"),
  * with a given standard deviation ("sigma").
  *
  * The bottom and top beyond a certain (n * sigma) interval
  * of the cumulative distribution are cut off here
  * to (arbitrarily and crudely, but somehow effectively) prevent extreme results,
  * and to define a finite range in which to sample x.
  *
  * The normal distribution formula:
  *
  * y = (1 / (sigma * sqrt(2 * pi))) * e^(-(x - mean)^2) / (2 * sigma^2))
  *
  * We cannot invert this formula by solving it for x (in real, non-complex numbers),
  * because we run into a square root of a negative while trying.
  * So let's use an alternative method.
  * The rejection sampling (aka acceptance-rejection) method
  * is straight forward and easy to understand:
  * http://en.wikipedia.org/wiki/Rejection_sampling
  */
class GaussRandom(mean: Double, sigma: Double, val definitionRangeSigmas: Int = 5) {
  if (sigma <= 0) {
    throw new IllegalArgumentException()
  }

  private val _definitionRangeHalf = definitionRangeSigmas * sigma
  private val _seed = mean.toLong ^ sigma.toLong ^ System.currentTimeMillis;
  private val _random = new Random(_seed);

  /**
    * @return the probability of value x for this distribution
    */
  def probability(x: Double): Double = {
    return (1 / (sigma * math.sqrt(2 * math.Pi))) *
      math.exp(-math.pow(x - mean, 2) / (2 * math.pow(sigma, 2)))
  }

  /**
    * The maximum value of this distribution function,
    * which is also the probability of the mean.
    */
  val maxProbability = probability(mean)

  /**
    * @return a random value with this distribution
    */
  def next(): Double = {
    while (true) {
      val x = mean + (_random.nextDouble() * 2 * _definitionRangeHalf) - _definitionRangeHalf
      val y = probability(x)
      val r = _random.nextDouble() * maxProbability
      if (r < y) {
        return x
      }
    }
    // unreachable:
    throw new RuntimeException
  }

}
