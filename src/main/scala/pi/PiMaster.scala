package pi


import akka.actor._
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import pi.PiMaster._
/**
  * @author rsouza on 24/06/17.
  */
object PiMaster {
  def props(nrOfWorkers:Option[Int]) = Props(new PiMaster(nrOfWorkers.getOrElse(5)))

  case class Calculate(id:Long, nrOfElements: Int)
  case class Work(id:Long, start: Int, nrOfElements: Int)
  case class Result(id:Long, value: Double)
  case class CalculationWork(id:Long, pi:Double, nrOfElementsLeft: Int, senderRef:ActorRef)
  case class CalculationResult(id:Long, pi: Double)
}

class PiMaster(nrOfWorkers: Int) extends Actor with ActorLogging {

  var pi:Double = 0
  var calculatedMap = Map.empty[Long, CalculationWork]
  val workRouter = {
    val workers = Vector.fill(nrOfWorkers) {
      ActorRefRoutee(context.actorOf(PiWorker.props()))
    }
    Router(RoundRobinRoutingLogic(), workers)
  }

  override def receive: Receive = {
    case Calculate(id, nrOfElements) =>
      calculatedMap += id -> CalculationWork(id, 0, nrOfElements, sender())
      for(i <- 0 until nrOfElements)
        workRouter.route(Work(id, i * nrOfElements, nrOfElements), context.self)

    case Result(id, value) =>
      var calculationWork = calculatedMap(id)
      pi = calculationWork.pi + value
      calculationWork =  calculationWork.copy(pi = pi, nrOfElementsLeft = calculationWork.nrOfElementsLeft - 1)
      calculatedMap += id -> calculationWork
      if(calculationWork.nrOfElementsLeft == 0){
        calculationWork.senderRef ! CalculationResult(calculationWork.id, calculationWork.pi)
      }
  }
}
