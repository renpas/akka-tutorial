package pi

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import pi.PiMaster.{Calculate, CalculationResult}

/**
  * @author rsouza on 24/06/17.
  */
object PiApp extends App{

  class PiStarter extends Actor with ActorLogging{
    var start:Long = _
    override def receive: Receive = {
      case "start" =>
        val piMasterRef = system.actorOf(PiMaster.props(Some(50)))
        start = System.currentTimeMillis()
        piMasterRef ! Calculate(0, 10000)

      case CalculationResult(id, pi) =>
        log.info("Calculation [{}] finished {} took {} ms", id, pi, System.currentTimeMillis() - start)
//        log.info("Took {} ms", )
        context.system.terminate()
    }
  }

  val system = ActorSystem("PiSystem")
  system.actorOf(Props[PiStarter], "pistarter") ! "start"




}
