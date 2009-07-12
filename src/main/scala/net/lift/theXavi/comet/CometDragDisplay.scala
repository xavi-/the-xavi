package net.lift.theXavi.comet

import net.liftweb._
import http._
import js._
import JsCmds._

import net.liftweb.http.js.JE._

import scala.actors._
import scala.actors.Actor._
import scala.Nil

object isSender extends SessionVar(false)

case class PlaceShape(x: Int, y: Int)
case class MoveShape(moves: List[List[Number]])
case class AddListener(listener: Actor)

object ShapeTracker extends Actor {
  private var shapePos = (0, 0)
  private var listeners: List[Actor] = Nil
  
  this.start
   
  def act = loop {
    react {
      case PlaceShape(x, y) =>
        shapePos = (x, y)
        listeners.foreach(_ ! PlaceShape(x, y))
      case MoveShape(moves) =>
        shapePos = (moves.last(0).intValue, moves.last(1).intValue)
        listeners.foreach(_ ! MoveShape(moves))
      case AddListener(listener) =>
        listeners ::= listener
        listener ! PlaceShape(shapePos._1, shapePos._2)
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
      Script(Function("moveShape", List("points"), this.jsonCall("move", JsRaw("points"))))
    }
    <div id="shape" style={ "left: " + shapePos._1 + "px; top: " + + shapePos._2 + "px;" }></div>
  	</xml:group>
    
  override def highPriority() = {
    case PlaceShape(x, y) =>
      shapePos = (x, y)

      if(!isSender.get)
    	  partialUpdate(JsRaw("$('#shape').css({left: " + x + ", top: " + y + "})"))
      else
        isSender(false)
    case MoveShape(moves) =>
      shapePos = (moves.last(0).intValue, moves.last(1).intValue)

      if(!isSender.get) {
        var serial = moves.map(i => String.format("{ x: %s, y: %s, time: %s }",
                                                  i(0).intValue.toString, i(1).intValue.toString, i(2).longValue.toString))
    	  partialUpdate(JsRaw("receiveUpdate([" + serial.reduceLeft(_ + ", " + _) + "])"))
      } else {
        isSender(false)
      }
  }
  
  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, moves: List[List[Number]], _) =>
      isSender(true)
      ShapeTracker ! MoveShape(moves)
      Noop
    case j => println("poo: " + j); Noop
  }
}
