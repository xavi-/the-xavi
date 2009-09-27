package net.theXavi.snippet

import net.liftweb._

import http._
import util._
import Helpers._
import SHtml._
import js._
import JsCmds._
import JE._

import scala.xml._
import net.theXavi.comet._
import net.liftweb.http.provider.HTTPCookie

class ChatroomUI {
  def render(xml: NodeSeq): NodeSeq = {
    val roomName = S.param("name").openOr("").toLowerCase

    UserName.set(S.findCookie("name").map(_.value openOr ""))

    def sendLine(user: String) =
      JsCrVar("user", user) &
      Function("sendLine", List("line"),
               ajaxCall(JsRaw("line"),
                        s => { if(s != "") { ChatManager.rooms(roomName) ! AddLine(user, s) } ; Noop })._2)
    
    bind("chat", xml,
      AttrBindParam("name", Text(roomName), "name"),
      "title" -> S.param("name").openOr(""),
      "chat-text" -> ChatManager.rooms.get(roomName)
                                      .map(_.lines).getOrElse(Nil)
                                      .reverse.flatMap(u => <div class="line">{ u._1 + ": " + u._2 }</div>),
      "reply" -> { (x: NodeSeq) =>
        UserName.is match {
          case Some(user) => <xml:group>{ chooseTemplate("ch", "sendLine", x) ++ Script(sendLine(user)) }</xml:group>
          case None =>
            <xml:group>{
              chooseTemplate("ch", "enterName", x) ++
              Script(Function("enterName", List("userName"),
                              ajaxCall(JsRaw("userName"), u => { UserName.set(Some(u)) 
                                                                 S.addCookie(HTTPCookie("name", u))
                                                                 sendLine(u) })._2))
            }</xml:group>
        }
      }
    )
  }
}