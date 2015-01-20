package quickstart.action

import xitrum.{ Action, Config, Log, JSEngine, ReactRenderer}
import xitrum.annotation.GET


@GET("react")
class ReactIndex extends Action with ReactRenderer {
  override def layout = renderViewNoLayout()
  def execute() {
    val props = Map("msg1" -> "Hello", "msg2" -> "World")
    renderReactView("App", props)
    respondView()
  }
}

// Just execute javascript without React
@GET("execJS")
class JSIndex extends Action {
  override def layout = renderViewNoLayout()
  implicit val indexJS = "serverEval.js"
  def execute() {
    val html = JSEngine.evalJS(Map("_" -> "underscore"), "global._.map([1,2,3], function(i){return i*2;}).join('-')") match {
      case (0, result) => result
      case _           => "<div style='display:none;'>Error happen on Server-Side React Rendering</div>"
    }
    respondText(html)
  }
}