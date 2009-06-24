package net.lift.theXavi.comet

import net.liftweb._
import net.liftweb.util._
import http._
import js._
import JsCmds._
import util._

import net.liftweb.http.js.JE._

import scala.xml._
import scala.actors._
import scala.actors.Actor._

object isSender extends SessionVar(false)

case class MoveShape(x: Int, y: Int)
case class AddListener(listener: Actor)

object ShapeTracker extends Actor {
  private var shapePos = (0, 0)
  private var listeners: List[Actor] = Nil
  
  this.start
   
  def act = loop {
    react {
      case MoveShape(x, y) =>
        shapePos = (x, y)
        listeners.foreach(_ ! MoveShape(shapePos._1, shapePos._2))
      case AddListener(listener) =>
        listeners ::= listener
        listener ! MoveShape(shapePos._1, shapePos._2)
    }
  }
}

class CometDragDisplay extends CometActor {
  private var shapePos = (0, 0)
  
  override def localSetup() {
    ShapeTracker ! AddListener(this)
  }
  
  def render =
    <xml:group>
    {
      Script(Function("moveShape", List("x", "y"), this.jsonCall("move", JsRaw("[x, y]")))
      		 & JsRaw("$(document).ready(addDrag)"))
    }
    <div id="shape" style={"left: " + shapePos._1 + "px; top: " + shapePos._2 + "px"}></div>
  	</xml:group>
    
  override def highPriority() = {
    case MoveShape(x, y) =>
      shapePos = (x, y)
      if(!isSender.get)
    	  partialUpdate(JsRaw("$('#shape').css({ left: " + shapePos._1 + ", top: " + shapePos._2 + "})"))
	  else
         isSender(false)
  }
  
  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, (x:Number) :: (y:Number) :: Nil, _) =>
      isSender(true)
      ShapeTracker ! MoveShape(x.intValue, y.intValue)
      Noop
    case j => println("poo: " + j); Noop
  }
}
