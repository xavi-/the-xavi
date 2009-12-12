package net.theXavi.comet

import net.liftweb._
import http._
import util._
import SHtml._
import js._
import JsCmds._
import JE._
import JSONParser._

import scala.actors._
import scala.actors.Actor._

case class ApplyOperations(senderId: Int, transId: Int, operations: List[Map[String, Any]])

object OperationsCoord extends Actor {
  var text: String = ""
  var transId: Int = 0;
  private var history: Map[Int, List[Map[String, Any]]] = Map(0 -> Nil)
  private var listeners: List[Actor] = Nil

  this.start
  
  def combineOperations(a: List[Map[String, Any]], b: List[Map[String, Any]]): List[Map[String, Any]] = {
    var rtn: List[Map[String, Any]] = Nil;
    
    var i = 0; var j = 0
    while(i < a.size && j < b.size) {
      if(i >= a.size){
        rtn = rtn ::: List(b(j)) ; j += 1
      } else if (j >= b.size) {
        rtn = rtn ::: List(a(i)) ; i += 1
      } else {
        val posA = a(i)("pos").asInstanceOf[Number].intValue
        val posB = b(j)("pos").asInstanceOf[Number].intValue
        
        (a(i)("cmd"), b(j)("cmd")) match {
          case ("del", "del") =>
            if(posA > posB) { rtn = rtn ::: List(a(i), b(j)) }
            else if(posA < posB) { rtn = rtn ::: List(b(j), a(i)) }
            else { rtn = rtn ::: List(a(i)) }
          case ("del", "ins") =>
            if(posA > posB) { rtn = rtn ::: List(a(i), b(j)) }
            else if(posA < posB) { rtn = rtn ::: List(b(j), a(i)) }
            else { rtn = rtn ::: List(a(i), b(j)) }
          case ("ins", "del") =>
            if(posA > posB) { rtn = rtn ::: List(a(i), b(j)) }
            else if(posA < posB) { rtn = rtn ::: List(b(j), a(i)) }
            else { rtn = rtn ::: List(a(j), b(i)) }
          case ("ins", "ins") =>
            if(posA > posB) { rtn = rtn ::: List(a(i), b(j)) }
            else if(posA < posB) { rtn = rtn ::: List(b(j), a(i)) }
            else { 
              val strA = a(i)("val").asInstanceOf[String]
              val strB = b(j)("val").asInstanceOf[String]
              if(strA > strB) { rtn = rtn ::: List(a(i), b(j)) }
              else if(strA < strB) { rtn = rtn ::: List(b(j), a(i)) }
              else { rtn = rtn ::: List(a(i)) }
            }
        }

        i += 1 ; j += 1
      }
    }

    rtn
  }

  def adjustOperations(base: List[Map[String, Any]], top: List[Map[String, Any]]): List[Map[String, Any]] = {
    def applySingleOperation(base: Map[String, Any], top: Map[String, Any]): Map[String, Any] = {
      val posBase = base("pos").asInstanceOf[Number].intValue
      val posTop = top("pos").asInstanceOf[Number].intValue

      (base("cmd"), top("cmd")) match {
        case ("del", "del") =>
          if(posBase > posTop) { top }
          else if(posBase < posTop) { Map("cmd" -> "del", "pos" -> (posTop - 1)) }
          else { Map() }
        case ("del", "ins") =>
          if(posBase > posTop) { top }
          else if(posBase < posTop) { Map("cmd" -> "ins", "pos" -> (posTop - 1), "val" -> top("val")) }
          else { top }
        case ("ins", "del") =>
          if(posBase > posTop) { top }
          else if(posBase < posTop) { Map("cmd" -> "del", "pos" -> (posTop - 1)) }
          else { Map("cmd" -> "del", "pos" -> (posTop + 1)) }
        case ("ins", "ins") =>
          if(posBase > posTop) { top }
          else if(posBase < posTop) { Map("cmd" -> "ins", "pos" -> (posTop + 1), "val" -> top("val")) }
          else if(base("val") == top("val")){ Map() }
          else { top }
      }
    }

    var rtn: List[Map[String, Any]] = top
    var tmp: List[Map[String, Any]] = Nil

    for(bs <- base) {
      for(rt <- rtn) { tmp = tmp ::: List(applySingleOperation(bs, rt)) }

      rtn = tmp
    }

    rtn
  }

  def applyOperations(ops: List[Map[String, Any]]) = {
    var tmp = text;
    for(op <- ops) {
      val pos = op("pos").asInstanceOf[Number].intValue
      op("cmd") match {
        case "ins" => tmp = tmp.substring(0, pos) + op("val").asInstanceOf[String] + tmp.substring(pos)
        case "del" => tmp = tmp.substring(0, pos) + tmp.substring(pos + 1)
      }
      println("op: " + op)
    }

    tmp
  }

  def act = loop {
    react {
      case ApplyOperations(senderId, baseId, ops) =>
        var tmpId = baseId + 1
        var adjustedOps = ops
println("ops: " + ops)
        while(tmpId <= transId) {
          adjustedOps = adjustOperations(history(tmpId), adjustedOps)
          tmpId += 1
        }
 println("adjustedOps: " + adjustedOps)
        text = applyOperations(adjustedOps)
        transId += 1;
        history += transId -> adjustedOps
println("\n\n text: " + text + "\n")
        listeners.foreach(_ ! ApplyOperations(senderId, baseId, ops))
      case AddListener(listener) =>
        listeners ::= listener
      case RemoveListener(listener) =>
        listeners -= listener
      case a => println("bad track: " + a)
    }
  }
}

class OperationsDisplay extends CometActor {
  override def localSetup() {
    OperationsCoord ! AddListener(this)
  }

  override def localShutdown() {
    OperationsCoord ! RemoveListener(this)
  }

  override def render = scala.xml.NodeSeq.Empty

  override def highPriority = {
    case ApplyOperations(senderId, transId, ops) =>
      partialUpdate(Call("receiveOperations",
                         senderId,
                         JsArray(ops.map( o => { JsObj("cmd" -> o("cmd").toString, "val" -> o("val").toString) }):_*),
                         OperationsCoord.transId))
  }
}