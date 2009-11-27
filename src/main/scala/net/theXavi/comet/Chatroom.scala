package net.theXavi.comet

import net.liftweb._
import http._
import js._
import JsCmds._
import JE._

object UserName extends SessionVar[Option[String]](None)

object ChatManager extends RoomManager[ChatRoom] {
  override def createRoom(name: String) = new ChatRoom(name)
}

case class AddLine(user: String, lines: String)
class ChatRoom(val name: String) extends Room[ChatClient] {
  var lines: List[(String, String)] = Nil

  def actions = {
    case AddLine(user, line) =>
      this.lines = ((user, line) :: lines).take(20)
      clients.foreach(_ ! AddLine(user, line))
  }
}

class ChatClient extends Client {
  val manager = ChatManager

  override def highPriority = {
    case AddLine(user, line) =>
      if(user != UserName.is.getOrElse("")) { partialUpdate(Call("addLine", user, line)) }
  }
}