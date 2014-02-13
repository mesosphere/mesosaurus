package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

class Resources(val cpus :Int, val mem :Int) {
	
	def this(offer :Offer) = this(offer.getCpus(), offer.getMem())
 	
	def <=(that :Resources) :Boolean = {
		return this.cpus <= that.cpus &&
			   this.mem <= that.mem
	}
	
	def -(that :Resources) :Resources = {
		val cpus = this.cpus - that.cpus
		val mem = this.mem - that.mem
		return new Resources(cpus, mem)
	}
}
