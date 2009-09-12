package net.lift.theXavi.comet

import net.liftweb._
import http._
import util._
import js._
import JsCmds._
import JE._

import scala.actors._
import scala.actors.Actor._

case class PiecePlaces(senderId: String, places: Map[String, (Int, Int, JsArray)])
case class MovePieces(senderId: String, moves: Map[String, List[List[Number]]])

object PieceTracker extends Actor {
  private val rand = new java.util.Random
  private var pieces = Map("pieceBW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("bee", "white", "1")),
                           "pieceTW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("beetle", "white", "1")),
                           "pieceTW2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("beetle", "white", "2")),
                           "pieceAW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "white", "1")),
                           "pieceAW2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "white", "2")),
                           "pieceAW3" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "white", "3")),
                           "pieceGW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "white", "1")),
                           "pieceGW2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "white", "2")),
                           "pieceGW3" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "white", "3")),
                           "pieceSW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("spider", "white", "1")),
                           "pieceSW2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("spider", "white", "2")),
                           "pieceMW1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("mosquito", "white", "1")),
                           "pieceBB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("bee", "black", "1")),
                           "pieceTB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("beetle", "black", "1")),
                           "pieceTB2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("beetle", "black", "2")),
                           "pieceAB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "black", "1")),
                           "pieceAB2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "black", "2")),
                           "pieceAB3" -> (rand.nextInt(400), rand.nextInt(400), JsArray("ant", "black", "3")),
                           "pieceGB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "black", "1")),
                           "pieceGB2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "black", "2")),
                           "pieceGB3" -> (rand.nextInt(400), rand.nextInt(400), JsArray("grasshopper", "black", "3")),
                           "pieceSB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("spider", "black", "1")),
                           "pieceSB2" -> (rand.nextInt(400), rand.nextInt(400), JsArray("spider", "black", "2")),
                           "pieceMB1" -> (rand.nextInt(400), rand.nextInt(400), JsArray("mosquito", "black", "1")))

  private var listeners: List[Actor] = Nil

  this.start

  def act = loop {
    react {
      case BroadcastPlaces(senderId) =>
        listeners.foreach(_ ! PiecePlaces(senderId, pieces))
      case MovePieces(senderId, moves) =>
        for((name, posList) <- moves) {
          pieces = (pieces(name) = (posList.last(0).intValue, posList.last(1).intValue, pieces(name)._3))
        }
        listeners.foreach(_ ! MovePieces(senderId, moves))
      case AddListener(listener) =>
        listeners ::= listener
      case a => println("bad track: " + a)
    }
  }
}

class HiveDisplay extends CometActor {
  override def localSetup() {
    PieceTracker ! AddListener(this)
  }

  override def render =
    Script(Function("moveShape", List("points"), this.jsonCall("move", JsRaw("points"))) &
           Function("getPlacements", Nil, this.jsonCall("places")))

  override def highPriority() = {
    case PiecePlaces(senderId, places) =>
      if(senderId != this.uniqueId) { continue }

      val jsPlaces = JsObj(places.toSeq.map(x => (x._1, JsObj("x" -> x._2._1, "y" -> x._2._2, "type" -> x._2._3))):_*)
      partialUpdate(Call("receivePlacements", jsPlaces))
    case MovePieces(senderId, moves) =>
      if(senderId == this.uniqueId) { continue }

      val jsMoves = JsObj(moves.toSeq.map(x =>
              (x._1, JsArray(x._2.map(pos
                      => JsObj("x" -> pos(0).intValue, "y" -> pos(1).intValue, "time" -> pos(2).longValue)):_*))):_*)
      partialUpdate(Call("receiveUpdate", jsMoves))
  }

  override def handleJson(in: Any): JsCmd = in match {
    case JsonCmd("move", _, moves: Map[String, List[List[Number]]], _) =>
      PieceTracker ! MovePieces(this.uniqueId, moves)
      Noop
    case JsonCmd("places", _, _, _) =>
      PieceTracker ! BroadcastPlaces(this.uniqueId)
      Noop
    case j => println("poo: " + j); Noop
  }
}