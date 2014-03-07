package io.mesosphere.mesosaurus

import scala.util._

/**
  * Deliver random numbers that are poisson-distributed around a given mean value.
  *
  * The top 0.1% of the cumulative distribution are cut off below
  * to (arbitrarily and crudely, but somehow effectively) prevent extremely large results.
  *
  * We use the inversion transformation method to derive random values from the distribution:
  * http://en.wikipedia.org/wiki/Inverse_transform_sampling
  * 
  * Poisson distribution formula, computing the probability that k random events happen during time t:
  *
  * 	Pk(t) = ((lambda * t)^k / k!) * e^(-lambda * t)
  *
  * Setting k = 0 yields the probability that no random events happen during time t:
  *
  *  	P0(t) = e^(-lambda * t)
  *
  * Resolving for t yields an event time function for any assumed probability:
  *
  * 	t = -ln(1 - p) / lambda
  *
  *  where p is any actual event distribution penetration value in the range [0.0|1.0[
  *  and lambda is the mean event rate 1/n,
  *  assuming on average n events occur per time unit in which t is expressed.
  *  Substituting mean for lambda yields:
  *
  *  t = -ln(1 - p) * mean
  */
class PoissonRandom(mean: Double) {
	private val _epsilon = 0.001
    private val _seed = mean.toLong ^ System.currentTimeMillis;
    private val _random = new Random(_seed);

    /**
     * @return a random value with this distribution
     */
    def next(): Double = {
        var p = 0.0
        do {
            p = _random.nextDouble() // in range [0.0|1.0]
        } while (p > 1 - _epsilon) // prevent extremely large and infinite end results caused by p close to 1.0

        val result = -math.log(1.0 - p) * mean;
        return result * 1.007 // compensate the actual mean for the truncated high end
    }

}
