import akka.actor._

/**
  * @author rsouza on 17/06/17.
  */
object ErrorHandling extends App{

  val system = ActorSystem("ErrorHandlingSystem")

  class SupervisingActor extends Actor{
    val child = context.actorOf(Props[SupervisedActor], "supervised-actor")

    override def receive: Receive = {
      case "failChild" => child ! "fail"
    }
  }

  class SupervisedActor extends Actor{
    override def preStart(): Unit = println("supervised actor started")
    override def postStop(): Unit = println("supervised actor stopped")

    override def receive: Receive = {
      case "fail" =>
        println ("supervise actor fails now")
        throw new Exception("I have failed T_T")
    }
  }

  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
  supervisingActor ! "failChild"

  system.terminate()

}
