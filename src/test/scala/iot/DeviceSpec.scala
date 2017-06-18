package iot

import akka.actor.ActorSystem
import akka.testkit._
import org.scalatest._
import scala.concurrent.duration._

/**
  * @author rsouza on 17/06/17.
  */
class DeviceSpec extends FlatSpec with Matchers {

  implicit val system = ActorSystem("testSystem")

  "A Device actor " should "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should  be (42)
    response.value should ===(None)
  }

  "A Device actor " should "reply with latest temperature readin" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.RecordTemperature(requestId = 1, 24.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

    deviceActor.tell(Device.ReadTemperature(requestId = 2), probe.ref)
    val response1 = probe.expectMsgType[Device.RespondTemperature]
    response1.requestId should be(2)
    response1.value should be(Some(24.0))

    deviceActor.tell(Device.RecordTemperature(requestId = 3, 55.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 3))

    deviceActor.tell(Device.ReadTemperature(requestId = 4), probe.ref)
    val response2 = probe.expectMsgType[Device.RespondTemperature]
    response2.requestId should be(4)
    response2.value should be(Some(55.0))
  }

  "A Device actor " should "rpely to registration request " in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("group", "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    probe.lastSender should be(deviceActor)
  }

  "A Device actor " should "ignore wrong registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "wrongDevice"), probe.ref)
    probe.expectNoMsg(500.milliseconds)

    deviceActor.tell(DeviceManager.RequestTrackDevice("group", "wrongDevice"), probe.ref)
    probe.expectNoMsg(500.milliseconds)
  }
}
