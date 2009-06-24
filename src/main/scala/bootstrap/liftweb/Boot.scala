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
                  Menu(Loc("Visual Sort", List("visual-sort"), "Visual Sort")) ::
                  Menu(Loc("Drag Shapes", List("drag-shapes", "index"), "Drag Shapes"),
                      Menu(Loc("Comet Drag", List("drag-shapes", "comet-drag"), "Comet Drag")),
                      Menu(Loc("Get the goal", List("drag-shapes", "get-the-goal"), "Get the goal")),
                      Menu(Loc("Rotate Image", List("drag-shapes", "rotate-image"), "Rotate Image")),
                      Menu(Loc("Find the Slant", List("drag-shapes", "slanted-box"), "Find the slanted box"))) ::
                  Menu(Loc("Key Remapper", List("key-remapper"), "Key Remapper")) ::
                  Menu(Loc("tv-downloader", List("tv-schedule-downloader"), "TV Schedule Downloader")) ::
                  Menu(Loc("About Me", List("about-me"), "About Me")) ::
                  Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))
  }
}

