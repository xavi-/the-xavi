package net.lift.theXavi.comet

import net.liftweb._
import http._
import util._
import Helpers._

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

case class SendRoom(room: ChatRoom, lines: List[String])
case class SendLine(lines: String)
class ChatRoom(val name: String) extends Actor {
  var clients: List[ChatClient] = Nil
  private var lines: List[String] = Nil

  this.start
  
  def act = loop {
    react {
      case AddClient(_, client) =>
        clients ::= client
        client ! SendRoom(this, lines)
      case RemoveClient(_, client) => 
        clients = clients.filter(_ != client)
      case SendLine(line) =>
        this.lines = (line :: lines).take(20)
        clients.foreach(_ ! SendLine(line))
    }
  }
}

class ChatClient extends CometActor {
  private lazy val _name = this.name.openOr("")
  private var lines: List[String] = Nil
  private var room: ChatRoom = null

  override def localSetup() {
    ChatManager ! AddClient(_name, this)
  }

  override def localShutdown() { println("\n shutting down chat client: " + _name)
    ChatManager ! RemoveClient(_name, this)
  }

  override def render =
    <div id="chat">{ lines.reverse.flatMap(l => <div class="line">{ l }</div>) }</div>

  override def highPriority = {
    case SendRoom(room, lines) =>
      this.room = room
      this.lines = lines
      reRender(true)
    case SendLine(line) =>
      this.lines = line :: lines
      reRender(true)
  }
}