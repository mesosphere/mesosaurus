package io.mesosphere.mesosaurus

import org.apache.mesos.Protos._

class Resources(val cpus: Double, val mem: Double) {

  def this(offer: Offer) = this(static.getCpus(offer), static.getMem(offer))

  def <=(that: Resources): Boolean = {
    return this.cpus <= that.cpus &&
      this.mem <= that.mem
  }

  def -(that: Resources): Resources = {
    val cpus = this.cpus - that.cpus
    val mem = this.mem - that.mem
    return new Resources(cpus, mem)
  }
}

private object static {
  private def getScalar(offer: Offer, name: String): Double = {
    var value = 0.0;
    for (i <- 0 until offer.getResourcesCount()) {
      val resource = offer.getResources(i)
      if (resource.getName() == name) {
        value += resource.getScalar().getValue()
      }
    }
    return value;
  }

  def getCpus(offer: Offer): Double = {
    return getScalar(offer, "cpus");
  }

  def getMem(offer: Offer): Double = {
    return getScalar(offer, "mem");
  }
}
