package io.mesosphere

import org.scalatest._
import org.scalatest.junit._
import org.junit.runner._
import io.mesosphere.mesosaurus._

@RunWith(classOf[JUnitRunner])
class GaussRandomTest extends FlatSpec with ShouldMatchers {

	"The arithmetic mean" should "converge to the expected mean" in {
		val tolerancePercent = 1
		val n = 100000
		for (exponent <- -3 until 6) {
			val expectedMean = math.pow(10.0, exponent)
			for (k <- 1 until 10) {
				val sigma = expectedMean / math.pow(2, k)
				val random = new GaussRandom(expectedMean, sigma)
	            var mean = 0.0
	            for (i <- 0 until n) {
	                mean += random.next()
	            }
	            mean /= n
	            val variancePercent = math.abs(1 - (mean / expectedMean)) * 100
	            if (variancePercent > tolerancePercent) {
		            println("expected mean:     " + expectedMean)
		            println("actual mean:       " + mean)
		            println("expected variance: " + tolerancePercent + " %")
		            println("actual variance:   " + variancePercent + " %")
		            println()	            	
		            fail()
	            }
			}
		}
	}

	"The standard deviation" should "converge to the expected standard deviation" in {
		val tolerancePercent = 1
		val n = 100000
		for (exponent <- -3 until 6) {
			val mean = math.pow(10.0, exponent)
            for (k <- 1 until 10) {
				val expectedSigma = mean / math.pow(2, k)
				val random = new GaussRandom(mean, expectedSigma)
	            var deltaSquares = 0.0
	            for (i <- 0 until n) {
	                deltaSquares += math.pow(random.next() - mean, 2)
	            }
	            val sigma = math.sqrt(deltaSquares / n)
	            val variancePercent = math.abs(1 - (sigma / expectedSigma)) * 100
	
	            if (variancePercent > tolerancePercent) {
		            println("expected sigma:    " + expectedSigma)
		            println("actual sigma:      " + sigma)
		            println("expected variance: " + tolerancePercent + " %")
		            println("actual variance:   " + variancePercent + " %")
		            println()
		            fail()
	            }
            }
		}
	}
	
	"The observed distribution" should "converge to the expected distibution" in {
		val tolerancePercent = 8
		val n = 1000000
		for (exponent <- -3 until 6) {
			val mean = math.pow(10.0, exponent)
            for (k <- 1 until 10) {
				val expectedSigma = mean / math.pow(2, k)
				val random = new GaussRandom(mean, expectedSigma)
				
				val halfRange = random.definitionRangeSigmas * expectedSigma
				val fullRange = 2 * halfRange
				val rangeStart = mean - halfRange

				val nBuckets = 100
				val buckets = new Array[Double](nBuckets)
				val bucketWidth = fullRange / nBuckets
				
	            for (i <- 0 until n) {
	                val index = ((random.next() - rangeStart) / bucketWidth).toInt
	                buckets(index) += 1
	            }	            
	            
				// We leave out the left and right 25% of tail buckets, 
				// because narrow confidence intervals for them 
				// would require many more experiment repetitions (n)
				// and we don't want to wait that long :-)
				for (i <- nBuckets / 4 until nBuckets * 3 / 4) {
					val x = rangeStart + (i * bucketWidth) + (bucketWidth / 2)
					val expectedHits = random.probability(x) * bucketWidth * n
					val variancePercent = math.abs(1 - (buckets(i) / expectedHits)) * 100

					if (variancePercent > tolerancePercent) {
						println("expected hits:     " + expectedHits)
						println("actual hits:       " + buckets(i))
						println("expected variance: " + tolerancePercent + " %")
						println("actual variance:   " + variancePercent + " %")
						println()
						fail()
					}
				}
            }
		}
	}

}
