package xitrum

import xitrum.util.SeriDeseri
import xitrum.view.Renderer

trait ReactRenderer extends Renderer {
  this: Action =>

  implicit val indexJS        = "serverEvalReact.js"
  private  val bufferForProp  = new StringBuilder
  private  val bufferForReact = new StringBuilder

  lazy val reactForView  = if (bufferForReact.isEmpty) "" else bufferForReact.toString
  lazy val propsForReact = if (bufferForProp.isEmpty) ""  else bufferForProp.toString

  def reactAddToView(reactHtml: String) {
    bufferForReact.append(reactHtml)
    // Don't append line break
  }

  def propsAddToView(reactClass: String, props:String) {
    bufferForProp.append(s"<script id='${reactClass}-props' type='text/plain' data-react-class='${reactClass}' data-json='${props}'></script>")
    bufferForProp.append("\n")
  }

  def propsAddToView(reactClass: String, props: Map[String, Any]) {
    val propsJson = SeriDeseri.toJson(props)
    propsAddToView(reactClass, props)
  }

  def renderReactView(reactClass: String, props: Map[String, Any]): Unit = {
    val deps      = Map(s"${reactClass}" -> s"./react/${reactClass}.jsx")

    val propsJson = SeriDeseri.toJson(props)
    propsAddToView(reactClass, propsJson)

    val html = JSEngine.evalJS(deps, s"React.renderToString(global.${reactClass}(JSON.parse('${propsJson}')))") match {
      case (0, result) => result
      case _           => "<div style='display:none;'>Error happen on Server-Side React Rendering</div>"
    }
    reactAddToView(html)
  }
}