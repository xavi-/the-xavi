package net.lift.theXavi.comet

import net.liftweb._
import net.liftweb.http.js._
import JsCmds._
import http._
import util._

import scala.actors._
import scala.actors.Actor._

import scala.collection.mutable.{Map}

object UserInfo extends SessionVar[ChatInfo](new ChatInfo)
class ChatInfo { var chat: Map[String, Actor] = Map(); var user: Box[String] = Empty }

case class AddClient(name: String, client: ChatClient)
case class RemoveClient(name: String, client: ChatClient)

object ChatManager extends Actor {
  private var rooms: Map[String, ChatRoom] = Map()

  this.start

  def act = loop {
    react {
      case AddClient(name, client) =>
        if(!rooms.contains(name)) { rooms(name) = new ChatRoom(name) }
        rooms(name) ! AddClient(name, client)
      case RemoveClient(name, client) =>
        rooms(name) ! RemoveClient(name, client)
    }
  }
}

case class SendRoom(room: ChatRoom, lines: List[String])
case class SendLine(lines: String)
class ChatRoom(val name: String) extends Actor {
  private var clients: List[ChatClient] = Nil
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
  var lines: List[String] = Nil
  var room: ChatRoom = null

  override def localSetup() {
    ChatManager ! AddClient(this.name.openOr(""), this)
  }
  
  override def render =
    <div id="chat">{ lines.reverse.flatMap(l => <span class="line">{ l }</span><br/>) }</div>

  override def highPriority = {
    case SendRoom(room, lines) =>
      this.room = room
      this.lines = lines
      UserInfo.is.chat(this.name.openOr("")) = room
      reRender(true)
    case SendLine(line) =>
      this.lines = line :: lines
      reRender(true)
  }
}