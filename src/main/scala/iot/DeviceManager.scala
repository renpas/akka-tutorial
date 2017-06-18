package iot

import akka.actor.Actor.Receive
import akka.actor._
import iot.DeviceManager.RequestTrackDevice

/**
  * @author rsouza on 17/06/17.
  */
object DeviceManager {

  def props(): Props = Props[DeviceManager]

  final case class RequestTrackDevice(groupId: String, deviceId: String)
  case object DeviceRegistered

}

class DeviceManager extends Actor with ActorLogging {
  var groupIdToActor = Map.empty[String, ActorRef]
  var actorToGroupId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("DeviceManager started")

  override def postStop(): Unit = log.info("DeviceManager stopped")

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(groupId, _) =>
      groupIdToActor.get(groupId) match {
        case Some(ref) =>
          ref forward trackMsg
        case None =>
          log.info("Creating device group actor for {}", groupId)

          val groupActor = context.actorOf(DeviceGroup.props(groupId))
          context.watch(groupActor)
          groupActor forward trackMsg
          groupIdToActor += groupId -> groupActor
          actorToGroupId += groupActor -> groupId
      }

    case Terminated(groupActor) =>
      val groupId = actorToGroupId(groupActor)
      log.info("Device group actor for {} has been terminated", groupId)
      actorToGroupId -= groupActor
      groupIdToActor -= groupId
  }
}