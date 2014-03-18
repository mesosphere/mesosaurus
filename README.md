# Mesosaurus

A benchmarking 
[framework](https://github.com/mesosphere/scala-sbt-mesos-framework.g8) 
for [Mesos](http://mesos.apache.org).

## Status

This is work in progress in its early stages. Expect bugs and missing features.

## Overview

The Mesosaurus framework creates work loads that can be configured to simulate 
the behavior of other frameworks on a Mesos cluster.

## Goals

We intend to come up with configurations that approximate Marathon, Chronos, 
Hadoop, MPI, Storm, Spark, and many others. By running multiple instances of 
Mesosaurus concurrently, it should be possible to simulate large production 
systems comprising of several frameworks competing for resources. It is our goal
that Metrics from such runs can then be used for cluster and Mesos core 
performance analysis.

## Current Features
* Poisson-distributed task arrival times.
* Normal/Gauss-distributed task durations and resource consumptions.
* Tasks that claim resource offers and 
  use up computer resources in the amounts specified.
* A configurable load factor for actual CPU load.

## Planned Features
* Read in external traces from other systems and create tasks to simulate the 
  recreation of past scenarios.
* Store traces.
* Real-time plotting of interesting metrics.

## Installation
1. Install Mesos on a cluster - or simply on a single machine
   if you just want to try it out. In any case, is easiest to follow the 
   [instructions at Mesosphere](http://mesosphere.io/downloads/) to do this.
2. Download, install and run the latest version of Mesosaurus:
    <pre><code>
   TODO
    </pre></code>

## Building from Source
1. Check out the latest source code from its git repository:
    <pre><code>
    git clone https://github.com/mesosphere/mesosaurus.git
    </pre></code>

2. Build the task executor, which is written in C++ and 
   install its binary where Mesosaurus can find it later on.
    <pre><code>
    cd mesosaurus/task
    make
    </pre></code>

3. Manually copy the task executor to the */tmp* directory 
   on every slave machine in your Mesos cluster.
   On your local machine, this would be:
    <pre><code>
    cp mesosaurus-task /tmp/
    cd ..
    </pre></code>
   This is only a temporary solution until we have a better one.

4. Download and install libraries that Mesosaurus depends on and 
   translate the Scala source code to bytecode:
    <pre><code>
    sbt compile
    </code></pre>

5. If you have a Mesos master running on your local machine 
   you can simply execute Mesosaurus with default values for all settings:
    <pre><code>
    sbt run
    </code></pre>

6. As a more complex example, you can run 10 tasks 
   that take on average 1 second 
   and arrive on average every 2 seconds
   on a Mesos installation with its master at a specified IP address and port:
    <pre><code>
    sbt "run -tasks 10 -duration 1000 -arrival 2000 -master 127.0.0.1:5050"
    </code></pre>

## Eclipse Support
1. Install [ScalaIDE](http://scala-ide.org) for Eclipse.
2. Install [sbteclipse](https://github.com/typesafehub/sbteclipse).
3. To create an eclipse project that you can import into Eclipse 
   for Mesosaurus development or simply for source code browsing:
    <pre><code>
    sbt eclipse
    </code></pre>
   This creates a file with the name ".project" in the Mesosaurus directory.
4. Import the Mesosaurus project using the *Import Wizard* in Eclipse,
   indicating *Existing Projects into Workspace* and 
   selecting your Mesosaurus directory as the base of your existing project.

## Usage
Execute the Mesosaurus program just to peruse a list 
of its available command line options:
    <pre><code>
    sbt "run -h"
    </code></pre>

## Coding Conventions
We try to follow the usual 
[Scala Style Guide](http://docs.scala-lang.org/style/), 
but we insist on two things:
1. No TAB character is allowed anywhere in source code.
2. Indentation must be 4 spaces, not 2.

Furthermore:
* We are still on the fence wrt. where exactly 
  the colon in front of type expressions should be.

More rules may be added or changed later 
and global reformatting may occur to ensure compliance.
