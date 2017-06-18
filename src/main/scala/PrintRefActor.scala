import akka.actor._

/**
  * @author rsouza on 17/06/17.
  */
object PrintRefActor extends  App{

  class PrintMyActorRefActor(val code: Int) extends Actor{

    override def preStart: Unit = {
      println(s"Before gets any ref ${code}")
    }

    override def receive: Receive = {
      case "printit" =>
        val secondRef = context.actorOf(Props (new PrintMyActorRefActor(2)), "second-actor")
        println(s"Second: $secondRef")
        secondRef ! "mensagem"
      case "mensagem" =>
        println(s"Recebi essa porra ${context.self}")
    }

    override def postStop: Unit = {
      println(s"Finishing ${code}")
    }
  }

  val system = ActorSystem("PrintRefSystem")
  val firstRef = system.actorOf(Props(new PrintMyActorRefActor(1)), "first-actor")
  println(s"First: $firstRef")

  firstRef  ! "printit"

  //system.stop(firstRef)
  system.stop(firstRef)
}
