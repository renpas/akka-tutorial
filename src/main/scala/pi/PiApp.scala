package pi

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import pi.PiMaster.{Calculate, CalculationResult}

/**
  * @author rsouza on 24/06/17.
  */
object PiApp extends App{

  class PiStarter extends Actor with ActorLogging{
    override def receive: Receive = {
      case "start" =>
        val piMasterRef = system.actorOf(PiMaster.props(Some(10)))
        piMasterRef ! Calculate(0, 10000)

      case CalculationResult(id, pi) =>
        log.info("Calculation [{}] finished {}", id, pi)
        context.system.terminate()
    }
  }

  val system = ActorSystem("PiSystem")
  system.actorOf(Props[PiStarter], "pistarter") ! "start"




}
