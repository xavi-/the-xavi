package net.lift.theXavi.snippet

import net.liftweb._

import http._
import util._
import Helpers._
import SHtml._
import js._
import JsCmds._
import JE._

import scala.xml._
import net.lift.theXavi.comet._

class ChatroomUI {
  def render(xml: NodeSeq): NodeSeq = {
    val roomName = S.param("name").openOr("").toLowerCase

    def sendLine(user: String) =
      Function("sendLine", List("line"),
                          ajaxCall(JsRaw("line"),
                                   s => { if(s != "") { UserInfo.is.chat(roomName) ! SendLine(user + ": " + s) } ; Noop })._2)
    bind("chat", xml,
      AttrBindParam("name", Text(roomName), "name"),
      "title" -> S.param("name").openOr(""),
      "reply" -> { (x: NodeSeq) =>
        UserInfo.is.user match {
          case Full(user) => <xml:group> { chooseTemplate("ch", "sendLine", xml) ++ Script(sendLine(user)) }</xml:group>
          case _ =>
            <xml:group>{
              chooseTemplate("ch", "enterName", xml) ++
              Script(Function("enterName", List("userName"),
                              ajaxCall(JsRaw("userName"),
                                       u => { UserInfo.is.user = Box.!!(u) ; sendLine(u) })._2))
            }</xml:group>
        }
      }
    )
  }
}