package net.theXavi.comet

import net.liftweb._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import http._
import js._
import util._
import SHtml._
import JsCmds._

import net.liftweb.http.js.JE._

import scala.xml._

import scala.actors._
import scala.actors.Actor._

case class MoveShape(uniqueId: String, x: Int, y: Int)

object CometDragTracker extends Actor {
  var shapePos = (0, 0)
  private var listeners: List[Actor] = Nil
  
  this.start
   
  def act = loop {
    react {
      case MoveShape(uniqueId, x, y) =>
        shapePos = (x, y)
        listeners.foreach(_ ! MoveShape(uniqueId, shapePos._1, shapePos._2))
      case AddListener(listener) =>
        listeners ::= listener
      case RemoveListener(listener) =>
        listeners -= listener
    }
  }
}

class CometDrag extends CometActor {
  override def localSetup() {
    CometDragTracker ! AddListener(this)
  }

  override def localShutdown() {
    CometDragTracker ! RemoveListener(this)
  }

  def render =
    <xml:group>
    {
      Script(Function("moveShape", List("x", "y"), this.jsonCall("move", JsRaw("[x, y]")))
      		 & JsRaw("$(document).ready(addDrag)"))
    }
    <div id="shape" style={"left: " + CometDragTracker.shapePos._1 + "px;" +
                           "top: " + CometDragTracker.shapePos._2 + "px"}></div>
  	</xml:group>
    
  override def highPriority() = {
    case MoveShape(uniqueId, x, y) if(uniqueId != this.uniqueId) =>
      partialUpdate(JsRaw("$('#shape').css({ left: " + x + ", top: " + y + "})"))
  }
  
  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, (x:Number) :: (y:Number) :: Nil, _) =>
      CometDragTracker ! MoveShape(this.uniqueId, x.intValue, y.intValue)
      Noop
    case j => println("poo: " + j); Noop
  }
}