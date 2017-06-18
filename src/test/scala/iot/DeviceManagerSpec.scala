package iot

import akka.actor._
import akka.testkit._
import org.scalatest._
import scala.concurrent.duration._

/**
  * @author rsouza on 18/06/17.
  */
class DeviceManagerSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("deviceManagerSpec")

  it should "be able to register new decives" in {
    val probe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    val deviceActor2 = probe.lastSender
    deviceActor1 should not be deviceActor2

    // check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }


}
