package iot

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}

/**
  * @author rsouza on 17/06/17.
  */
object IotSupervisor {
  def props(): Props = Props(new IotSupervisor())
}

class IotSupervisor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("IoT Application started")
  override def postStop(): Unit = log.info("IoT Application stopped")

  override def receive: Receive = Actor.emptyBehavior
}
