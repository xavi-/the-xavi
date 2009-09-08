package bootstrap.liftweb

import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._

class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("net.lift.theXavi")
    
    LiftRules.uriNotFound.prepend{
      case (req, _) => PermRedirectResponse("404", req)
    }

    LiftRules.rewrite.append{
      case RewriteRequest(ParsePath("chat" :: name :: Nil, _, _, _), _, _)
        if(name != "index" && name != "chatrooms") =>
          RewriteResponse("chat" :: "chatrooms" :: Nil, Map("name" -> name))
    }
    
    // Build SiteMap
    val pages = Menu(Loc("Home", List("index"), "Home")) ::
                Menu(Loc("Quick Chat", List("chat", "index"), "Quick Chat"),
                    Menu(Loc("Chatrooms", List("chat", "chatrooms"), "Chatrooms", Hidden))) ::
                Menu(Loc("Drag Shapes", List("drag-shapes", "index"), "Drag Shapes"),
                    Menu(Loc("Comet Drag", List("drag-shapes", "comet-drag"), "Comet Drag")),
                    Menu(Loc("Get the goal", List("drag-shapes", "get-the-goal"), "Get the goal")),
                    Menu(Loc("Rotate Image", List("drag-shapes", "rotate-image"), "Rotate Image")),
                    Menu(Loc("Find the Slant", List("drag-shapes", "slanted-box"), "Find the slanted box"))) ::
                Menu(Loc("Articles", List("articles", "index"), "Articles"),
                    Menu(Loc("operation-not-supported", List("articles", "operation-is-not-supported-code-9"), "Google Analytics Adventures"))) ::
                Menu(Loc("Key Remapper", List("key-remapper"), "Key Remapper")) ::
                Menu(Loc("Visual Sort", List("visual-sort"), "Visual Sort")) ::
                Menu(Loc("tv-downloader", List("tv-schedule-downloader"), "TV Schedule Downloader")) ::
                Menu(Loc("About Me", List("about-me"), "About Me")) ::
                Menu(Loc("404", List("404"), "404", Hidden)) ::
                Nil
    
    LiftRules.setSiteMap(SiteMap(pages:_*))
  }
}

