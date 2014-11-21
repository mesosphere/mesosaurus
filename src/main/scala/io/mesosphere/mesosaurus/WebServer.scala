package io.mesosphere.mesosaurus

import tiscaf._

object WebServer extends HServer {

    //def _port = 8080
    def ports = Set(Port._port)

    def url(): String = {
        return "http://" + java.net.InetAddress.getLocalHost().getHostAddress() + ":" + Port._port
    }

    override protected def startStopListener {
        // do not start the daemon stop thread
    }

    object StaticApp extends HApp {
        object StaticLet extends let.FsLet {
            protected def dirRoot = "./task"
        }

        def resolve(req: HReqData) = Some(StaticLet)

        override def buffered: Boolean = true // ResourceLet needs buffered or chunked be set
    }

    def apps = Seq(StaticApp)

}
