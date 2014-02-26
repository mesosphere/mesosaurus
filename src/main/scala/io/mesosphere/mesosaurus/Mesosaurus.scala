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

    val defaultSettings = ConfigFactory.parseString("""
        mesos {
          	master = "localhost:5050"
    	}
    """)

    val failoverTimeoutMilliseconds = 2 * 60 * 1000 // 2 minutes

    private class UnsignedInt extends ArgumentType[Integer] {
    	override def convert(parser: ArgumentParser, argument :Argument, value :String): Integer = {
    		try {
            	val n: Integer = Integer.parseInt(value)
            	if (n < 0) {
                	throw new ArgumentParserException(String.format("%d must not be negative", n), parser)
            	}
            	return n
        	} catch {
        		case e: NumberFormatException => throw new ArgumentParserException(e, parser)
        	}
    	}
    }

    private def defineCommandLineOptions(parser: ArgumentParser): Unit = {
	    parser.addArgument("-tasks").help("# of tasks to launch")
    	
    	val subparsers = parser.addSubparsers().help("sub-command help")
    	
    	val simpleParser = subparsers.addParser("simple").help("simple task generator")

    	val poissonParser = subparsers.addParser("poisson").help("poisson task generator")
    	poissonParser.addArgument("-arrive").`type`(new UnsignedInt()).choices(Arguments.range(Integer.valueOf(0), Integer.valueOf(Integer.MAX_VALUE))).help("mean task arrival time in millisecond")
    }

    private def parseCommandLine(arguments: Array[String]): TaskGenerator = {
        var requestedTasks = 100
        var taskGenerator: TaskGenerator = null
        var meanArrivalMilliseconds = 10
        val parser = ArgumentParsers.newArgumentParser("mesosaurus")
            .defaultHelp(true)
            .description("benchmarking framework for Mesos")
        defineCommandLineOptions(parser)
        try {
            val namespace = parser.parseArgs(arguments)
        }
        catch {
            case e: ArgumentParserException =>
                parser.handleError(e)
                System.exit(1)
        }
        taskGenerator = new SimpleTaskGenerator(requestedTasks)
        taskGenerator = new PoissonTaskGenerator(requestedTasks, meanArrivalMilliseconds)
        return taskGenerator
    }

    // Execution entry point
    def main(arguments: Array[String]): Unit = {
        val taskGenerator = parseCommandLine(arguments)
        val scheduler = new MesosaurusScheduler(taskGenerator)

        val frameworkName = "Mesosaurus (Scala)"
        log.info("Hello from framework [{}]!", frameworkName)

        val frameworkInfo = FrameworkInfo.newBuilder()
            .setName(frameworkName)
            .setFailoverTimeout(failoverTimeoutMilliseconds)
            .setUser("") // let Mesos assign the user
            .setCheckpoint(false)
            .build
        val config = ConfigFactory.load.getConfig("io.mesosphere.mesosaurus").withFallback(defaultSettings)
        val mesosMaster = config.getString("mesos.master")
        val schedulerDriver = new MesosSchedulerDriver(scheduler, frameworkInfo, mesosMaster)

        val driverStatus = schedulerDriver.run()
        val exitStatus = if (driverStatus == Status.DRIVER_STOPPED) 0 else 1
        schedulerDriver.stop()
        System.exit(exitStatus)
    }

}
