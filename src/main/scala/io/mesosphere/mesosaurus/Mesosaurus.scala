package io.mesosphere.mesosaurus

import com.typesafe.config._
import net.sourceforge.argparse4j._
import net.sourceforge.argparse4j.impl._
import net.sourceforge.argparse4j.inf._
import org.apache.mesos._
import org.apache.mesos.Protos._

/**
  * Mesos benchmarking framework
  * that generates and schedules tasks in configurable ways
  * to simulate the statistical behavior of other (production use) frameworks.
  */
object Mesosaurus extends Logging {
	
	// All time values in this entire program are in milliseconds, so:
	private val SECOND = 1000
	private val MINUTE = 60 * SECOND
	
	// Default configuration values:
	private val DEFAULT_MESOS_MASTER = "localhost:5050"
	private val DEFAULT_FAILOVER_TIMEOUT = 2 * MINUTE
	private val DEFAULT_NUMBER_OF_TASKS = 10
	private val DEFAULT_TASK_DURATION = 1 * SECOND
	private val DEFAULT_MEAN_ARRIVAL_TIME = SECOND / 10

	// A new command line parameter type for argparse4j that forces Int values to be positive
    private object UnsignedInteger extends ArgumentType[Int] {
    	override def convert(parser: ArgumentParser, argument :Argument, value :String): Int = {
    		try {
            	val n: Int = Integer.parseInt(value)
            	if (n < 0) {
                	throw new ArgumentParserException(String.format("%d must not be negative", Integer.valueOf(n)), parser)
            	}
            	return n
        	} catch {
        		case e: NumberFormatException => throw new ArgumentParserException(e, parser)
        	}
    	}
    }
   
	// Command line option names:
    private val MASTER = "-master"
    private val FAILOVER = "-failover"
    private val TASKS = "-tasks"
    private val DURATION = "-duration"
    private val GENERATOR = "generator"    	
    private val SIMPLE = "simple"
    private val POISSON = "poisson"
    private val ARRIVE = "-arrive"

    private def defineCommandLineOptions(parser: ArgumentParser): Unit = {
    	parser.addArgument(MASTER).help("Mesos master IP:port")
    	parser.addArgument(FAILOVER).`type`(UnsignedInteger).help("failover timeout ms")
    	parser.addArgument(TASKS).`type`(UnsignedInteger).help("# tasks to launch")
    	parser.addArgument(DURATION).`type`(UnsignedInteger).help("mean task duration ms")
    	
    	val subparsers = parser.addSubparsers().dest(GENERATOR).title("subcommands").description("alternative task generators")
 
    	val simpleParser = subparsers.addParser(SIMPLE).help("simple task generator")

    	val poissonParser = subparsers.addParser(POISSON).help("poisson task generator")
    	poissonParser.addArgument(ARRIVE).`type`(UnsignedInteger).help("mean task arrival time in millisecond")
    }

    // Because Scala cannot disambiguate the overloaded Java method 
    // net.sourceforge.argparse4j.inf.Argument.setDefault()
    // we set default values AFTER parsing,
    // using these helper functions:
    
    private def getString(options :Namespace, name: String, defaultValue :String) :String = {
    	return if (options.get(name) != null) options.getString(name) else defaultValue
    }
    
    private def getInt(options :Namespace, name: String, defaultValue :Int) :Int = {
    	return if (options.get(name) != null) options.getInt(name).intValue() else defaultValue
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
        	options.getString(GENERATOR) match {
        		case SIMPLE => 
        			return (master, failover, new SimpleTaskGenerator(tasks, duration))
        		case POISSON => 
        			val meanArrival = getInt(options, ARRIVE, DEFAULT_MEAN_ARRIVAL_TIME)
        			return (master, failover, new PoissonTaskGenerator(tasks, duration, meanArrival))
        		case _ =>
        			throw new ArgumentParserException("no task generator specified", parser)
        	}
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

        val driverStatus = schedulerDriver.run()
        val exitStatus = if (driverStatus == Status.DRIVER_STOPPED) 0 else 1
        schedulerDriver.stop()
        System.exit(exitStatus)
    }
}
