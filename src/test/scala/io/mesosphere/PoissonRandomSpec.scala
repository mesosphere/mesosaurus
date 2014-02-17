package io.mesosphere

import org.scalatest._
import org.scalatest.junit._
import org.junit.runner._
import io.mesosphere.mesosaurus._

@RunWith(classOf[JUnitRunner])
class PoissonRandomTest extends FlatSpec with ShouldMatchers {
    val tolerancePercent = 2
    val n = 1000000

    "The arithmetic mean in " + n + " attempts" should "converge to the expected mean within " + tolerancePercent + " %" in {
        for (exponent <- -3 until 6) {
            val expectedMean = math.pow(10.0, exponent)
            val random = new PoissonRandom(expectedMean);
            var mean = 0.0
            for (i <- 0 until n) {
                mean += random.next()
            }
            mean /= n
            val variancePercent = math.abs(1 - (mean / expectedMean)) * 100

            println("expected mean: " + expectedMean)
            println("actual mean:   " + mean)
            println("variance:      " + variancePercent + " %")
            println()

            assert(variancePercent <= tolerancePercent)
        }
    }

    
    "The standard deviation in " + n + " attempts" should "converge to the expected standard deviation within " + tolerancePercent + " %" in {
        for (exponent <- -3 until 6) {
            val mean = math.pow(10.0, exponent)
            val expectedSigma = mean // In a Poisson distribution, mean and standard deviation are the same
            val random = new PoissonRandom(mean);
            var deltaSquares = 0.0
            for (i <- 0 until n) {
                deltaSquares += math.pow(random.next() - mean, 2)
            }
            val sigma = math.sqrt(deltaSquares / n)
            val variancePercent = math.abs(1 - (sigma / expectedSigma)) * 100

            println("expected sigma: " + expectedSigma)
            println("actual sigma:   " + sigma)
            println("variance:       " + variancePercent + " %")
            println()

            assert(variancePercent <= tolerancePercent)
        }
    }

}
