package quickstart.action

import java.io.File

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import akka.pattern.ask
import akka.util.Timeout

import com.typesafe.jse.{ Trireme, Rhino, CommonNode, Engine, Node }
import com.typesafe.jse.Engine.JsExecutionResult

import xitrum.{ Action, Config, Log }
import xitrum.annotation.GET
import xitrum.util.SeriDeseri
import xitrum.view.Renderer

trait JSEngine {

  def evalJS(deps: Map[String, String], js: String, indexFile: String = "serverEval.js"): (Int, String) = {
    implicit val timeout = Timeout(3.seconds)
    val engine      = Config.actorSystem.actorOf(Node.props())
    val indexJSFile =
      if (Config.productionMode) new File(Action.getClass.getResource(indexFile).toURI)
      else new File(s"${xitrum.root}/src/main/resources/${indexFile}")

    val res = for (
      result <- (engine ? Engine.ExecuteJs(indexJSFile, immutable.Seq(SeriDeseri.toJson(deps), js), timeout.duration)).mapTo[JsExecutionResult]
    ) yield {
      result.exitValue match {
        case 0        =>
          (0, new String(result.output.toArray, "UTF-8"))
        case err: Int =>
          Log.error(new String(result.error.toArray, "UTF-8"))
          (err, "Error at server-side js evaluation")
      }
    }
    Await.result(res, 3.second)
  }

}

trait ReactRenderer extends Renderer with JSEngine {
  this: Action =>

  private val bufferForProp  = new StringBuilder
  private val bufferForReact = new StringBuilder

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

    val html = evalJS(deps, s"React.renderToString(global.${reactClass}(JSON.parse('${propsJson}')))", "serverReact.js") match {
      case (0, result) => result
      case _           => "<div style='display:none;'>Error happen on Server-Side React Rendering</div>"
    }
    reactAddToView(html)
  }
}

@GET("react")
class ReactIndex extends Action with ReactRenderer {
  override def layout = renderViewNoLayout()
  def execute() {
    val props = Map("msg1" -> "Hello", "msg2" -> "World")
    renderReactView("App", props)
    respondView()
  }
}