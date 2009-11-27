package net.theXavi.comet

import net.liftweb._
import http._
import util._
import Helpers._

import scala.xml._
import scala.actors._
import scala.actors.Actor._

case class GarbageCollect
case class AddClient(name: String, client: Client)
case class RemoveClient(name: String, client: Client)

trait RoomManager[R <: Room[_]] extends Actor {
  var rooms: Map[String, R] = Map()

  def createRoom(name: String): R;

  this.start
  ActorPing.scheduleAtFixedRate(this, GarbageCollect, 10 minutes, 10 minutes)

  def act = loop {
    react {
      case AddClient(name, client) =>
        if(!rooms.contains(name)) { rooms += name -> createRoom(name) }
        rooms(name) ! AddClient(name, client)
      case RemoveClient(name, client) =>
        rooms(name) ! RemoveClient(name, client)
      case GarbageCollect =>
        println("\n\nGarbage Collecting!!")
        rooms foreach ( pair => if(pair._2.clients.size == 0) { println("Closed room: " + pair._1)
                                  pair._2 ! ShutDown
                                  rooms -= pair._1
                                } else { println("Open room: " + pair._1 + "; count: " + pair._2.clients.size) })
    }
  }
}

trait Room[C <: Client] extends Actor {
  val name: String
  var clients: List[Client] = Nil

  this.start

  def actions: PartialFunction[Any, Unit];

  private val clientService: PartialFunction[Any, Unit] = {
    case AddClient(_, client) =>
      clients ::= client
    case RemoveClient(_, client) =>
      clients = clients.filter(_ != client)
  }

  def act = loop { react { clientService orElse actions } }
}

trait Client extends CometActor {
  val manager: RoomManager[_];

  override def localSetup() {
    manager ! AddClient(this.name.openOr(""), this)
  }

  override def localShutdown() { println("\n shutting down chat client: " + this.name.openOr(""))
    manager ! RemoveClient(this.name.openOr(""), this)
  }

  def render = NodeSeq.Empty
}