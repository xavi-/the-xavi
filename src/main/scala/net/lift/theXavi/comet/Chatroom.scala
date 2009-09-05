package net.lift.theXavi.comet

import net.liftweb._
import http._
import util._
import Helpers._

import scala.actors._
import scala.actors.Actor._

object UserInfo extends SessionVar[ChatInfo](new ChatInfo)
class ChatInfo { var chatClients: Map[String, ChatClient] = Map(); var user: Box[String] = Empty }

case class GarbageCollect
case class AddClient(name: String, client: ChatClient)
case class RemoveClient(name: String, client: ChatClient)

object ChatManager extends Actor {
  var rooms: Map[String, ChatRoom] = Map()

  this.start
  ActorPing.scheduleAtFixedRate(this, GarbageCollect, 10 seconds, 10 seconds)
  
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
                                } )
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

  override def localShutdown() { println("\n shutting down chat client")
    ChatManager ! RemoveClient(_name, this)
    UserInfo.is.chatClients -= _name
  }

  override def render =
    <div id="chat">{ lines.reverse.flatMap(l => <span class="line">{ l }</span><br/>) }</div>

  override def highPriority = {
    case SendRoom(room, lines) =>
      this.room = room
      this.lines = lines
      UserInfo.is.chatClients += _name -> this
      reRender(true)
    case SendLine(line) =>
      this.lines = line :: lines
      reRender(true)
  }
}