package io.mesosphere.mesosaurus

import com.typesafe.config._
import net.sourceforge.argparse4j._
import net.sourceforge.argparse4j.impl._
import net.sourceforge.argparse4j.inf._
import org.apache.mesos._
import org.apache.mesos.Protos._
import java.net.URI

/**
  * Mesos benchmarking framework
  * that generates and schedules tasks in configurable ways
  * to simulate the statistical behavior of other (production use) frameworks.
  */
object Mesosaurus extends Logging {

  // All time values in this entire program are in milliseconds, so:
  private val SECOND = 1000
  private val MINUTE = 60 * SECOND

  // Naming conventions adopted from Mesos APIs:
  // "cpus": a number of "CPU core shares". Examples: 
  //   - 1.0 sums up to one core. 
  //   - If you use a quarter each of three cores, then you have 0.75 "shares".
  // "mem": RAM measured in M bytes, so 1 "mem" is (1024 * 1024) bytes

  // Default configuration values:
  private val DEFAULT_MESOS_MASTER = "localhost:5050"
  private val DEFAULT_FAILOVER_TIMEOUT = 2 * MINUTE
  private val DEFAULT_NUMBER_OF_TASKS = 10
  private val DEFAULT_TASK_DURATION = 1 * SECOND
  private val DEFAULT_TASK_ARRIVAL_TIME = SECOND / 10
  private val DEFAULT_CPU_LOAD = 0.5
  private val DEFAULT_CPUS = 1
  private val DEFAULT_MEM = 128
  private val DEFAULT_SIGMA_FACTOR = 5

  // A new command line parameter type for argparse4j that forces Int values to be positive
  private object UnsignedInteger extends ArgumentType[Int] {
    override def convert(parser: ArgumentParser, argument: Argument, value: String): Int = {
      try {
        val n: Int = java.lang.Integer.parseInt(value)
        if (n < 0) {
          throw new ArgumentParserException(f"$n%d must not be negative", parser)
        }
        return n
      }
      catch {
        case e: NumberFormatException => throw new ArgumentParserException(e, parser)
      }
    }
  }

  // A new command line parameter type for argparse4j that forces Long values to be positive
  private object UnsignedLong extends ArgumentType[Long] {
    override def convert(parser: ArgumentParser, argument: Argument, value: String): Long = {
      try {
        val n: Long = java.lang.Long.parseLong(value)
        if (n < 0) {
          throw new ArgumentParserException(f"$n%d must not be negative", parser)
        }
        return n
      }
      catch {
        case e: NumberFormatException => throw new ArgumentParserException(e, parser)
      }
    }
  }

  // A new command line parameter type for argparse4j that forces Double values to be positive
  private object UnsignedDouble extends ArgumentType[Double] {
    override def convert(parser: ArgumentParser, argument: Argument, value: String): Double = {
      try {
        val n = java.lang.Double.parseDouble(value)
        if (n < 0.0) {
          throw new ArgumentParserException(f"$n%f must not be negative", parser)
        }
        return n
      }
      catch {
        case e: NumberFormatException => throw new ArgumentParserException(e, parser)
      }
    }
  }

  // Command line option names:
  private val MASTER = "master"
  private val FAILOVER = "failover"
  private val TASKS = "tasks"
  private val DURATION = "duration"
  private val DURATION_SIGMA = "duration_sigma"
  private val ARRIVAL = "arrival"
  private val LOAD = "load"
  private val CPUS = "cpus"
  private val CPUS_SIGMA = "cpus_sigma"
  private val MEM = "mem"
  private val MEM_SIGMA = "mem_sigma"

  private def addOption(parser: ArgumentParser, optionName: String): Argument = {
    return parser.addArgument("-" + optionName)
  }

  private def defineCommandLineOptions(parser: ArgumentParser): Unit = {
    addOption(parser, MASTER).help("Mesos master IP:port")
    addOption(parser, FAILOVER).`type`(UnsignedInteger).help("failover timeout in ms")
    addOption(parser, TASKS).`type`(UnsignedInteger).help("# tasks to launch")
    addOption(parser, DURATION).`type`(UnsignedInteger).help("mean task duration in ms")
    addOption(parser, DURATION_SIGMA).`type`(UnsignedInteger).help("task duration standard deviation in ms")
    addOption(parser, ARRIVAL).`type`(UnsignedInteger).help("mean task arrival time in ms")
    addOption(parser, LOAD).`type`(UnsignedDouble).help("load factor for every utilized CPU")
    addOption(parser, CPUS).`type`(UnsignedDouble).help("mean # of cpus")
    addOption(parser, CPUS_SIGMA).`type`(UnsignedDouble).help("cpus standard deviation")
    addOption(parser, MEM).`type`(UnsignedLong).help("mean # MB of memory")
    addOption(parser, MEM_SIGMA).`type`(UnsignedLong).help("memory standard deviation in MB")
  }

  // Because Scala cannot disambiguate the overloaded Java method 
  // net.sourceforge.argparse4j.inf.Argument.setDefault()
  // we set default values AFTER parsing,
  // using these helper functions:

  private def getString(options: Namespace, name: String, defaultValue: String): String = {
    return if (options.get(name) != null) options.getString(name) else defaultValue

  }

  private def getInt(options: Namespace, name: String, defaultValue: Int): Int = {
    return if (options.get(name) != null) options.getInt(name).intValue() else defaultValue
  }

  private def getLong(options: Namespace, name: String, defaultValue: Long): Long = {
    return if (options.get(name) != null) options.getLong(name).intValue() else defaultValue
  }

  private def getDouble(options: Namespace, name: String, defaultValue: Double): Double = {
    return if (options.get(name) != null) options.getDouble(name).doubleValue() else defaultValue
  }

  private def parseCommandLine(arguments: Array[String]): (String, Int, TaskGenerator) = {
    val parser = ArgumentParsers.newArgumentParser("mesosaurus")
      .defaultHelp(true)
      .description("benchmarking framework for Mesos")
    defineCommandLineOptions(parser)
    try {
      val options = parser.parseArgs(arguments)
      val master = getString(options, MASTER, DEFAULT_MESOS_MASTER)
      val failover = getInt(options, FAILOVER, DEFAULT_FAILOVER_TIMEOUT)
      val tasks = getInt(options, TASKS, DEFAULT_NUMBER_OF_TASKS)
      val duration = getInt(options, DURATION, DEFAULT_TASK_DURATION)
      val arrival = getInt(options, ARRIVAL, DEFAULT_TASK_ARRIVAL_TIME)
      val durationSigma = getInt(options, DURATION_SIGMA, duration / DEFAULT_SIGMA_FACTOR)
      if (durationSigma <= 0) {
        throw new ArgumentParserException("duration standard deviation must be > 0", parser)
      }
      val load = getDouble(options, LOAD, DEFAULT_CPU_LOAD)
      if (load < 0 || load > 1) {
        throw new ArgumentParserException("load must be between 0.0 and 1.0", parser)
      }
      val cpus = getDouble(options, CPUS, DEFAULT_CPUS)
      val cpusSigma = getDouble(options, CPUS_SIGMA, cpus / DEFAULT_SIGMA_FACTOR)
      if (cpusSigma <= 0) {
        throw new ArgumentParserException("cpus standard deviation must be > 0", parser)
      }
      val mem = getLong(options, MEM, DEFAULT_MEM)
      val memSigma = getLong(options, MEM_SIGMA, mem / DEFAULT_SIGMA_FACTOR)
      if (memSigma <= 0) {
        throw new ArgumentParserException("mem standard deviation must be > 0", parser)
      }

      val artifacts: Seq[URI] = Nil // TODO: add URI for the task program

      val taskGenerator = new TaskGenerator(
        artifacts,
        tasks,
        duration,
        durationSigma,
        arrival,
        load,
        cpus,
        cpusSigma,
        mem,
        memSigma
      )

      return (master, failover, taskGenerator)
    }
    catch {
      case e: ArgumentParserException =>
        parser.handleError(e)
    }
    System.exit(1)
    return null
  }

  def main(arguments: Array[String]): Unit = {
    val (mesosMaster, failoverTimeout, taskGenerator) = parseCommandLine(arguments)
    val scheduler = new MesosaurusScheduler(taskGenerator)

    val frameworkName = "Mesosaurus (Scala)"
    log.info("Hello from framework {}!", frameworkName)

    val frameworkInfo = FrameworkInfo.newBuilder()
      .setName(frameworkName)
      .setUser("") // necessary for Mesos to fill in the current user
      .setFailoverTimeout(failoverTimeout)
      .setCheckpoint(false)
      .build
    val schedulerDriver = new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster)

    // WebServer.start
    val driverStatus = schedulerDriver.run()
    val exitStatus = if (driverStatus == Status.DRIVER_STOPPED) 0 else 1
    schedulerDriver.stop()
    // WebServer.stop
    System.exit(exitStatus)
  }
}
