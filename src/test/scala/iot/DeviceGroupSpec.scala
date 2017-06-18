package iot

import akka.actor._
import akka.testkit._
import org.scalatest._
import scala.concurrent.duration._

/**
  * @author rsouza on 17/06/17.
  */
class DeviceGroupSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("deviceGroupSpec")

  it should "be able to register a device actor " in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    val deviceActor2 = probe.lastSender
    deviceActor1 should not be deviceActor2

    // check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }

  it should "ignore requests for wrong groupId" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device1"), probe.ref)
    probe.expectNoMsg(500.milliseconds)
  }

  it should "return same actor for same deviceId" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender

    deviceActor1 should ===(deviceActor2)
  }

  it should "be able to list active devices " in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1", "device2")))
  }

  it should "be able to list active devices after one shuts down" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val toShutDown = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1", "device2")))

    probe.watch(toShutDown)
    toShutDown ! PoisonPill
    probe.expectTerminated(toShutDown)

    // using awaitAssert to retry because it might take longer for the groupActor
    // to see the Terminated, that order is undefined
    probe.awaitAssert{
      groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 1), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 1, Set("device2")))
    }
  }
}
