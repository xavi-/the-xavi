package net.lift.theXavi.comet

import net.liftweb._
import http._
import util._
import Helpers._
import js._
import JsCmds._
import JE._

import scala.xml._
import scala.actors._
import scala.actors.Actor._

object UserName extends SessionVar[Option[String]](None)

case class GarbageCollect
case class AddClient(name: String, client: ChatClient)
case class RemoveClient(name: String, client: ChatClient)

object ChatManager extends Actor {
  var rooms: Map[String, ChatRoom] = Map()

  this.start
  ActorPing.scheduleAtFixedRate(this, GarbageCollect, 10 minutes, 10 minutes)
  
  def act = loop {
    react {
      case AddClient(name, client) =>
        if(!rooms.contains(name)) { rooms += name -> new ChatRoom(name) }
        rooms(name) ! AddClient(name, client)
      case RemoveClient(name, client) =>
        rooms(name) ! RemoveClient(name, client)
      case GarbageCollect =>
        println("\n\nGarbage Collecting!!")
        rooms foreach ( pair => if(pair._2.clients.size == 0) { 
                                  println("Closed room: " + pair._1)
                                  pair._2 ! ShutDown
                                  rooms -= pair._1
                                } else { println("Open room: " + pair._1 + "; count: " + pair._2.clients.size) })
    }
  }
}

case class AddLine(user: String, lines: String)
class ChatRoom(val name: String) extends Actor {
  var clients: List[ChatClient] = Nil
  var lines: List[(String, String)] = Nil

  this.start
  
  def act = loop {
    react {
      case AddClient(_, client) =>
        clients ::= client
      case RemoveClient(_, client) => 
        clients = clients.filter(_ != client)
      case AddLine(user, line) =>
        this.lines = ((user, line) :: lines).take(20)
        clients.foreach(_ ! AddLine(user, line))
    }
  }
}

class ChatClient extends CometActor {
  override def localSetup() {
    ChatManager ! AddClient(this.name.openOr(""), this)
  }

  override def localShutdown() { println("\n shutting down chat client: " + this.name.openOr(""))
    ChatManager ! RemoveClient(this.name.openOr(""), this)
  }

  def render = NodeSeq.Empty

  override def highPriority = {
    case AddLine(user, line) =>
      if(user != UserName.is.getOrElse("")) { partialUpdate(Call("addLine", user, line)) }
  }
}