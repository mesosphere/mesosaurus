package io.mesosphere.mesosaurus

import scala.util._

/**
  * Deliver random numbers that are poisson-distributed around a given mean "lambda".
  *
  * Cutting off the top 1% of the cumulative distribution,
  * to (arbitrarily, but somehow effectively) bound extremely large results.
  */
class PoissonRandom(lambda: Double) {

  private val _seed = lambda.toLong ^ System.currentTimeMillis;
  private val _random = new Random(_seed);

  def next(): Double = {
    // Exclude extremely large results:
    var r = 0.0
    do {
      _random.nextDouble()
    } while (r > 0.99);

    return math.log(1.0 - r) / (-lambda);
  }

}
