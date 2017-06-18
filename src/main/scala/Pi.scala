import akka.actor.Actor.Receive
import akka.actor._
import akka.routing.RoundRobinRoutingLogic
//import akka.routing.RoundRobinRouter

import scala.concurrent.duration._


sealed trait PiMessage

case object Calculate extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage
case class PiApproximation(pi: Double, duration: Duration)

class Master(nrOfWorkers: Int, nrOfMessage: Int,
             nrOfElemenets: Int, Listener: ActorRef)
  extends Actor
{
  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis()

  //val workerRouter = //context.actorOf(
   // Props[Worker].withRouter(RoundRobinRoutingLogic(), name = "workerRouter")
  //)
  override def receive: Receive = Actor.emptyBehavior
}


class Worker extends Actor
{


  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for(i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  override def receive: Receive = {
    case Work(start, nrOfElements) =>
      sender ! Result(calculatePiFor(start, nrOfElements))
  }

}