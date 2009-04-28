package bootstrap.liftweb

import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("net.lift.theXavi")

    // Build SiteMap
    val entries = Menu(Loc("Home", List("index"), "Home")) ::
                  Menu(Loc("Drag Shapes", List("drag-shapes"), "Drag Shapes")) ::
                  Menu(Loc("Key Remapper", List("key-remapper"), "Key Remapper")) ::
                  Menu(Loc("Half Baked Ideas", List("half-baked-ideas"), "Half Baked Ideas")) ::
                  Menu(Loc("3/4 Baked Ideas", List("three-quarter-baked-ideas"), "3/4 Baked Ideas")) ::
                  Menu(Loc("About Me", List("about-me"), "About Me")) ::
                  Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))
  }
}

