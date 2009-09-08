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
                        s => { if(s != "") { ChatManager.rooms(roomName) ! SendLine(user + ": " + s) } ; Noop })._2)
    
    bind("chat", xml,
      AttrBindParam("name", Text(roomName), "name"),
      "title" -> S.param("name").openOr(""),
      "reply" -> { (x: NodeSeq) =>
        UserName.is match {
          case Some(user) => <xml:group>{ chooseTemplate("ch", "sendLine", xml) ++ Script(sendLine(user)) }</xml:group>
          case None =>
            <xml:group>{
              chooseTemplate("ch", "enterName", xml) ++
              Script(Function("enterName", List("userName"),
                              ajaxCall(JsRaw("userName"), u => { UserName.set(Some(u)) ; sendLine(u) })._2))
            }</xml:group>
        }
      },
      "unload-code" -> Script(Function("shutDown", Nil,
                                       ajaxInvoke(() => { S.session.foreach(_.findComet("ChatClient")
                                                                             .filter(_.name == roomName)
                                                                             .foreach(_ ! ShutDown))
                                                          Noop })._2))
    )
  }
}