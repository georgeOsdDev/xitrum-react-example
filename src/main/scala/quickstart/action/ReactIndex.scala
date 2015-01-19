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

  private val buffer = new StringBuilder

  lazy val propsForView = if (buffer.isEmpty) "" else buffer.toString

  def propsAddToView(reactClass: String, props: Map[String, Any]) {
    val propsJson = SeriDeseri.toJson(props)
    propsAddToView(reactClass, props)
  }

  def propsAddToView(reactClass: String, props:String) {
    buffer.append(s"<script id='${reactClass}-props' type='text/plain' data-react-class='{reactClass}' data-json='${props}'></script>")
    buffer.append("\n")
  }

  def renderReactView(reactClass: String, props: Map[String, Any]): String = {
    val deps      = Map(s"${reactClass}" -> s"./react/${reactClass}.jsx")

    val propsJson = SeriDeseri.toJson(props)
    propsAddToView(reactClass, propsJson)

    evalJS(deps, s"React.renderToString(global.${reactClass}(JSON.parse('${propsJson}')))", "serverReact.js") match {
      case (0, result) => result
      case _           => "<div style='display:none;'>Error happen on Server-Side React Rendering</div>"
    }
  }
}

@GET("react")
class ReactIndex extends Action with ReactRenderer {
  override def layout = renderViewNoLayout()
  def execute() {
    val props = Map("msg1" -> "Hello", "msg2" -> "World")
    at("react") = renderReactView("App", props)
    respondView()
  }
}