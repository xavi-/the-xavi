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

class OperationalTransformsUI {
  val uniqueId = (new java.util.Random).nextInt(1999999999)

  def render =
    <xml:group>
      <textarea id="edit" rows="5" cols="80"
        value={ println("coord Text: " + OperationsCoord.text); OperationsCoord.text }
        transId={ OperationsCoord.transId }
        uniqueId={ uniqueId } /> {
      Script(Function("sendOperations", List("transId", "ops"),
                      ajaxCall(JsRaw("transId + ';' + ops"),
                               o => { val ops = JSONParser.parse(o.substring(o.indexOf(";") + 1))
                                            match { case Full(ops: List[Map[String, Any]]) => ops
                                                    case _ => Nil:List[Map[String, Any]] }
                                      val transId = o.substring(0, o.indexOf(";")).toInt;println("transId: " + transId + "; json: " + o.substring(o.indexOf(";") + 1))
                                      OperationsCoord ! ApplyOperations(uniqueId, transId, ops)
                                      Noop })._2)) }
    </xml:group>
}