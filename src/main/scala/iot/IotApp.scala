package iot

import akka.actor.ActorSystem

import scala.io.StdIn

/**
  * @author rsouza on 17/06/17.
  */
object IotApp {

  def main(args:Array[String]): Unit = {
    val system = ActorSystem("iot-system")

    try{
      //create top level supervisor
      val supervisor = system.actorOf(IotSupervisor.props())

      //Exit the system after ENTER is pressed
      StdIn.readLine()
    } finally {
      system.terminate()
    }

  }

}
