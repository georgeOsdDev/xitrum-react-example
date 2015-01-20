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

import xitrum.util.SeriDeseri

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

    case _            => Rhino.props()
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

  def evalJS(deps: Map[String, String], js: String)(implicit indexJS: String): (Int, String) = {
    // Do not reuse engine actor
    // https://github.com/georgeOsdDev/xitrum-react-example/issues/1
    val engine      = Config.actorSystem.actorOf(JSEngine.config.runtimeProps)
    val indexJSFile =
      if (Config.productionMode) new File(getClass().getClassLoader().getResource(indexJS).getFile())
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