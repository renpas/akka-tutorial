package iot

import akka.actor._
import iot.DeviceGroup.{ReplyDeviceList, RequestAllTemperatures, RequestDeviceList}
import iot.DeviceManager.RequestTrackDevice
import scala.concurrent.duration._

/**
  * @author rsouza on 17/06/17.
  */
object DeviceGroup{
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)
  final case class ReplyDeviceList(requestId:Long, ids: Set[String])

  final case class RequestAllTemperatures(requestId: Long)
  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIdToActor = Map.empty[String, ActorRef]
  var actorToDeviceId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg @ RequestTrackDevice(`groupId`, _) =>
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(deviceActor) =>
          deviceActor forward trackMsg
        case None =>
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
          context.watch(deviceActor)
          actorToDeviceId += deviceActor -> trackMsg.deviceId
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          deviceActor forward trackMsg
      }

    case msg @ RequestTrackDevice(_, _) =>
      log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}", msg.groupId, this.groupId)

    case RequestDeviceList(requestId) =>
      sender ! ReplyDeviceList(requestId, deviceIdToActor.keySet)

    case Terminated(deviceActor) =>
      val deviceId = actorToDeviceId(deviceActor)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceId -= deviceActor
      deviceIdToActor -= deviceId

    case RequestAllTemperatures(requestId) =>
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = actorToDeviceId,
        requestId = requestId,
        requester = sender(),
        3 seconds
      ))
  }
}
