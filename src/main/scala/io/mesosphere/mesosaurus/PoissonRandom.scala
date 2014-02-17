package io.mesosphere.mesosaurus

import scala.util._

/**
  * Deliver random numbers that are poisson-distributed around a given mean value.
  *
  * Cutting off the top 0.1% of the cumulative arrival time distribution,
  * to (arbitrarily and crudely, but somehow effectively) prevent extremely large results.
  *
  * Poisson distribution formula, computing the probability that k arrivals happen during time t:
  *
  * 	Pk(t) = ((lambda * t)^k / k!) * e^(-lambda * t)
  *
  * Setting k = 0 yields the probability that no arrivals happen during time t:
  *
  *  	P0(t) = e^(-lambda * t)
  *
  * Resolving for t yields an arrival time distribution:
  *
  * 	t = -ln(1 - p) / lambda
  *
  *  where p is any actual arrival distribution penetration value in the range [0.0|1.0[
  *  and lambda is the mean arrival rate 1/n,
  *  assuming on average n tasks arrive per time unit in which t is expressed.
  *  Substituting mean for lambda yields:
  *
  *  t = -ln(1 - p) * mean
  */
class PoissonRandom(mean: Double) {
    private val _seed = mean.toLong ^ System.currentTimeMillis;
    private val _random = new Random(_seed);

    def next(): Double = {
        var p = 0.0
        do {
            p = _random.nextDouble() // in range [0.0|1.0]
        } while (p > 0.999) // prevent extremely large and infinite end results caused by p close to 1.0

        val result = -math.log(1.0 - p) * mean;
        return result * 1.007 // compensate the actual mean for the truncated high end
    }

}
