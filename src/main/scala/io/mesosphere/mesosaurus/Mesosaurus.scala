package io.mesosphere.mesosaurus

import com.typesafe.config._
import org.apache.mesos._
import org.apache.mesos.Protos._
import org.apache.commons.cli

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

  private def parseCommandLine(args: Array[String]) = {
    val options = new cli.Options();
    options.addOption("simple", false, "simple task generator");
  }

  // Execution entry point
  def main(args: Array[String]): Unit = {
    parseCommandLine(args);

    val frameworkName = "Mesosaurus (Scala)"
    log.info("Hello from framework [{}]!", frameworkName)

    val requestedTasks = 10; // TODO: configurable
    //val taskSource = new SimpleTaskSource(requestedTasks);
    val taskSource = new PoissonTaskGenerator(requestedTasks, 100); // TODO: configurable
    val scheduler = new MesosaurusScheduler(taskSource)
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
    val exitStatus = if (driverStatus == Status.DRIVER_STOPPED) 0 else 1;
    schedulerDriver.stop();
    System.exit(exitStatus);
  }

}
