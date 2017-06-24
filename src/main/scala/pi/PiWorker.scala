package pi

import akka.actor._
import pi.PiMaster._
/**
  * @author rsouza on 24/06/17.
  */
object PiWorker {
  def props() = Props[PiWorker]
}

class PiWorker extends Actor with ActorLogging{

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    var acc = 0.0
    for (i ← start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }

  override def receive: Receive = {
    case Work(id, start, nrOfElements) =>
      sender ! Result(id, calculatePiFor(start, nrOfElements))
  }
}
