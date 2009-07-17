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

case class PlaceShape(places: Map[String, (Int, Int)])
case class MoveShape(moves: Map[String, List[List[Number]]])
case class AddListener(listener: Actor)

object ShapeTracker extends Actor {
  private var shapes = Map((1 to 5).map(x => ("shape"+x, (0,0))):_*)
  private var listeners: List[Actor] = Nil
  
  this.start
   
  def act = loop {
    react {
      case PlaceShape(places) =>
        shapes = places
        listeners.foreach(_ ! PlaceShape(places))
      case MoveShape(moves) =>
        for((name, posList) <- moves) { shapes = (shapes(name) = (posList.last(0).intValue, posList.last(1).intValue)) }
        listeners.foreach(_ ! MoveShape(moves))
      case AddListener(listener) =>
        listeners ::= listener
        listener ! PlaceShape(shapes)
      case a => println("bad track: " + a)
    }
  }
}

class CometDragDisplay extends CometActor {
  private var shapes: Map[String, (Int, Int)] = Map((1 to 5).map(x => ("shape"+x, (0,0))):_*)

  override def localSetup() {
    ShapeTracker ! AddListener(this)
  }
  
  def render =
    <xml:group>
    {
      Script(Function("moveShape", List("points"), this.jsonCall("move", JsRaw("points")))) ++
      shapes.map(x => <div id={x._1} style={ "left: " + x._2._1 + "px; top: " + x._2._2 + "px;" }></div>)
    }
  	</xml:group>
    
  override def highPriority() = {
    case PlaceShape(places) =>
      shapes = places
      if(!isSender.get)
    	  partialUpdate(JsRaw(places.map(x => "$('#" + x._1 + "').css({left: " + x._2._1 + ", top: " + x._2._2 + "}); ")
                      .reduceLeft(_ + _)))
      else
        isSender(false)
    case MoveShape(moves) =>
      for((name, posList) <- moves) { shapes = (shapes(name) = (posList.last(0).intValue, posList.last(1).intValue)) }

      if(!isSender.get) {
        var jsMoves = JsObj(moves.toSeq.map(x =>
                (x._1, JsArray(x._2.map(pos =>
                        JsObj("x" -> pos(0).toString, "y" -> pos(1).toString, "time" -> pos(2).toString)):_*))):_*)
        
    	  partialUpdate(JsRaw("receiveUpdate(" + jsMoves + ")"))
      } else {
        isSender(false)
      }
  }
  
  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, moves: Map[String, List[List[Number]]], _) =>
      isSender(true)
      ShapeTracker ! MoveShape(moves)
      Noop
    case j => println("poo: " + j); Noop
  }
}
