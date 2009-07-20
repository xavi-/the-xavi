package net.lift.theXavi.comet

import net.liftweb._
import http._
import js._
import JsCmds._

import net.liftweb.http.js.JE._

import scala.actors._
import scala.actors.Actor._
import scala.Nil

case class BroadcastPlaces(senderId: String)
case class ShapePlaces(senderId: String, places: Map[String, (Int, Int)])
case class MoveShapes(senderId: String, moves: Map[String, List[List[Number]]])
case class AddListener(listener: Actor)

object ShapeTracker extends Actor {
  private var shapes = Map((1 to 5).map(x => ("shape"+x, (0,0))):_*)
  private var listeners: List[Actor] = Nil
  
  this.start
   
  def act = loop {
    react {
      case BroadcastPlaces(senderId) =>
        listeners.foreach(_ ! ShapePlaces(senderId, shapes))
      case MoveShapes(senderId, moves) =>
        for((name, posList) <- moves) { shapes = (shapes(name) = (posList.last(0).intValue, posList.last(1).intValue)) }
        listeners.foreach(_ ! MoveShapes(senderId, moves))
      case AddListener(listener) =>
        listeners ::= listener
      case a => println("bad track: " + a)
    }
  }
}

class CometDragDisplay extends CometActor {
  override def localSetup() {
    ShapeTracker ! AddListener(this)
  }
  
  override def render =
    Script(Function("moveShape", List("points"), this.jsonCall("move", JsRaw("points"))) &
           Function("getPlacements", Nil, this.jsonCall("places")))
    
  override def highPriority() = {
    case ShapePlaces(senderId, places) =>
      if(senderId != this.uniqueId) { continue }

      val jsPlaces = JsObj(places.toSeq.map(x => (x._1, JsObj("x" -> x._2._1, "y" -> x._2._2))):_*)
      partialUpdate(Call("receivePlacements", jsPlaces))
    case MoveShapes(senderId, moves) =>
      if(senderId == this.uniqueId) { continue }

      val jsMoves = JsObj(moves.toSeq.map(x =>
              (x._1, JsArray(x._2.map(pos
                      => JsObj("x" -> pos(0).intValue, "y" -> pos(1).intValue, "time" -> pos(2).longValue)):_*))):_*)
      partialUpdate(Call("receiveUpdate", jsMoves))
  }
  
  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, moves: Map[String, List[List[Number]]], _) =>
      ShapeTracker ! MoveShapes(this.uniqueId, moves)
      Noop
    case JsonCmd("places", _, _, _) =>
      ShapeTracker ! BroadcastPlaces(this.uniqueId)
      Noop
    case j => println("poo: " + j); Noop
  }
}