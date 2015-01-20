package xitrum

import java.io.File
import java.util.concurrent.TimeUnit

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

import com.typesafe.config.{Config => TConfig}
import com.typesafe.jse.{ Engine, CommonNode, JavaxEngine, Node, PhantomJs, Rhino, Trireme}
import com.typesafe.jse.Engine.JsExecutionResult

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout

import xitrum.annotation.GET
import xitrum.util.SeriDeseri
import xitrum.view.Renderer

class JSEngineConfig(config: TConfig) {
  val timeout            = if (config.hasPath("timeout")) config.getLong("timeout") else 1000L
  val runtimeProps:Props = config.getString("runtime") match {
    case "JavaxEngine" => JavaxEngine.props()
    case "Rhino"       => Rhino.props()
    case "Trireme"     => Trireme.props()

    // These Runtime needs to be installed on system
    case "CommonNode" => CommonNode.props()
    case "PhantomJs"  => PhantomJs.props()
    case "Node"       => Node.props()

    case _            => Trireme.props()
  }
}

object JSEngine {

  val config:JSEngineConfig = try {
    new JSEngineConfig(Config.application.getConfig("jsengine"))
  } catch {
    case NonFatal(e) =>
    Config.exitOnStartupError("Could not load jsengine config", e)
    throw e
  }

  implicit val timeout = Timeout(JSEngine.config.timeout, TimeUnit.MILLISECONDS)
  val engine     = Config.actorSystem.actorOf(JSEngine.config.runtimeProps)

  def evalJS(deps: Map[String, String], js: String)(implicit indexJS: String): (Int, String) = {
    val indexJSFile =
      if (Config.productionMode) new File(JSEngine.getClass.getResource(indexJS).toURI)
      else new File(s"${xitrum.root}/src/main/resources/${indexJS}")

      val res = for (
        result <- (engine ? Engine.ExecuteJs(indexJSFile, immutable.Seq(SeriDeseri.toJson(deps), js), timeout.duration)).mapTo[JsExecutionResult]
      ) yield {
        result.exitValue match {
          case 0 =>
          (0, new String(result.output.toArray, "UTF-8"))
          case err: Int =>
          Log.error(new String(result.error.toArray, "UTF-8"))
          (err, "Error at server-side js evaluation")
        }
      }
      Await.result(res, JSEngine.config.timeout.milliseconds)
    }
}


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