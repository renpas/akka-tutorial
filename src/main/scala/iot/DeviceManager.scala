package iot

import akka.actor._
import iot.DeviceManager.{ReplyDeviceGroupList, RequestDeviceGroupList, RequestTrackDevice}

/**
  * @author rsouza on 17/06/17.
  */
object DeviceManager {

  def props(): Props = Props(new DeviceManager(deviceGroupFactory))

  def deviceGroupFactory(context:ActorContext, groupId:String) = context.actorOf(DeviceGroup.props(groupId))

  final case class RequestTrackDevice(groupId: String, deviceId: String)
  case object DeviceRegistered

  final case class RequestDeviceGroupList(requestId: Long)
  final case class ReplyDeviceGroupList(requestId:Long, ids: Set[String])

}

class DeviceManager(deviceGroupFactory: (ActorContext, String) => ActorRef) extends Actor with ActorLogging {
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
          val groupActor:ActorRef = deviceGroupFactory(context, groupId)
          context.watch(groupActor)
          groupActor forward trackMsg
          groupIdToActor += groupId -> groupActor
          actorToGroupId += groupActor -> groupId
      }

    case RequestDeviceGroupList(requestId) =>
      sender ! ReplyDeviceGroupList(requestId, groupIdToActor.keySet)

    case Terminated(groupActor) =>
      val groupId = actorToGroupId(groupActor)
      log.info("Device group actor for {} has been terminated", groupId)
      actorToGroupId -= groupActor
      groupIdToActor -= groupId
  }
}
