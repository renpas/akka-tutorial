package iot

import akka.actor._
import akka.testkit._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._

/**
  * @author rsouza on 18/06/17.
  */
class DeviceGroupQuerySpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  implicit val system = ActorSystem("deviceGroupQuerySpec")

  var requester: TestProbe = _
  var device1:TestProbe = _
  var device2:TestProbe = _
  var queryActor:ActorRef = _


  override def beforeEach() = {
    requester = TestProbe()
    device1 = TestProbe()
    device2 = TestProbe()
    queryActor = system.actorOf(DeviceGroupQuery.props(
      actorToDeviceId = Map(device1.ref -> "device1", device2.ref -> "device2"),
      requestId = 1,
      requester = requester.ref,
      timeout = 1 seconds
    ))
    device1.expectMsg(Device.ReadTemperature(requestId = 0))
    device2.expectMsg(Device.ReadTemperature(requestId = 0))

  }

  it should "return temperature value for working devices" in {
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(2.0)), device2.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.Temperature(2.0)
      )
    ))
  }

  it should "return TemperatureNotAvailable for devices with no readings" in {
    queryActor.tell(Device.RespondTemperature(requestId = 0, None), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(2.0)), device2.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.TemperatureNotAvailable,
        "device2" -> DeviceGroup.Temperature(2.0)
      )
    ))

  }

  it should "return DeviceNotAvailable if device stops before answering" in {
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)
    device2.ref ! PoisonPill

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceNotAvailable
      )
    ))
  }

  it should "return temperature reading even if device stops after answering" in {
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(2.0)), device2.ref)
    device2.ref ! PoisonPill

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.Temperature(2.0)
      )
    ))
  }

  it should "return DeviceTimedOut if device does not answer in time" in {
    queryActor.tell(Device.RespondTemperature(requestId = 0, Some(1.0)), device1.ref)

    requester.expectMsg(DeviceGroup.RespondAllTemperatures(
      requestId = 1,
      temperatures = Map(
        "device1" -> DeviceGroup.Temperature(1.0),
        "device2" -> DeviceGroup.DeviceTimedOut
      )
    ))
  }


}
