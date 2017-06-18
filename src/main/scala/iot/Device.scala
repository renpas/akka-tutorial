package iot

import akka.actor._
import iot.Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}
import iot.DeviceManager.{DeviceRegistered, RequestTrackDevice}

/**
  * @author rsouza on 17/06/17.
  */
object Device{
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  final case class RecordTemperature(requestId: Long, value: Double)
  final case class TemperatureRecorded(requestId: Long)

  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value:Option[Double])
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging{
  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info("iot.Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("iot.Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case RequestTrackDevice(`groupId`, `deviceId`) =>
      sender ! DeviceRegistered

    case RequestTrackDevice(groupId, deviceId) =>
      log.warning("Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
        groupId, deviceId, this.groupId, this.deviceId
      )

    case RecordTemperature(id, value) =>
      log.info("Recorded temperature reading {} with {}", value, id)
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)

    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)
  }
}
