package iot

import akka.actor._
import akka.testkit._
import iot.DeviceManager.ReplyDeviceGroupList
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

    managerActor.tell(DeviceManager.RequestDeviceGroupList(0), probe.ref)
    val deviceList = probe.expectMsgType[DeviceManager.ReplyDeviceGroupList]
    deviceList.ids should be(Set("group"))

    // check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }

  it should " be able to list groups even after removing one " in {
    val probe = TestProbe()

    var groupIdToDeviceGroupRef = Map.empty[String, ActorRef]

    val deviceGroupFactory = (context:ActorContext, groupId:String) => {
      val deviceGroupRef = context.actorOf(DeviceGroup.props(groupId))

      groupIdToDeviceGroupRef += groupId -> deviceGroupRef

      deviceGroupRef
    }

    val managerActor = system.actorOf(Props( new DeviceManager(deviceGroupFactory)))

    managerActor.tell(DeviceManager.RequestTrackDevice("group1", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    managerActor.tell(DeviceManager.RequestTrackDevice("group1", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    managerActor.tell(DeviceManager.RequestTrackDevice("group2", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    managerActor.tell(DeviceManager.RequestDeviceGroupList(0), probe.ref)
    val deviceList1 = probe.expectMsgType[DeviceManager.ReplyDeviceGroupList]

    deviceList1 should be(ReplyDeviceGroupList(0, Set("group1", "group2")))

    groupIdToDeviceGroupRef("group2") ! PoisonPill

    probe.awaitAssert{
      managerActor.tell(DeviceManager.RequestDeviceGroupList(1), probe.ref)
      val deviceList2 = probe.expectMsgType[DeviceManager.ReplyDeviceGroupList]
      deviceList2 should ===(ReplyDeviceGroupList(1, Set("group1")))
    }
  }

}