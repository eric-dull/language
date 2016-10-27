import akka.actor.ActorSystem
import colossus._
import core._
import network.eic.language.EntityProcessor
import service._
import protocols.http._
import UrlParsing._
import HttpMethod._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

import JsonUtil._

case class EntityRequest(text: String, lang: String)
class Server(context: ServerContext) extends HttpService(context) {

  val entityProcessor = new EntityProcessor
  val jsonHeaders = HttpHeaders(
    HttpHeader("Content-Type", "application/json; charset=utf-8")
  )

  val htmlHeaders = HttpHeaders(
    HttpHeader("Content-Type", "text/html; charset=utf-8")
  )


  def handle = {

    case request @ Post on Root / "entities" / "list" => {
      val entityRequest = getEntityRequest(request)
      val entities = entityProcessor.getEntities(entityRequest.text)
      Callback.successful(request.ok(JsonUtil.toJson(entities), jsonHeaders))
    }

    case request @ Post on Root / "entities" / "positions_by_type" => {
      val entityRequest = getEntityRequest(request)
      val entities = entityProcessor.getEntityPositionsByType(entityRequest.text)
      Callback.successful(request.ok(JsonUtil.toJson(entities), jsonHeaders))
    }

    case request @ Post on Root / "entities" / "highlighted_text" => {
      val entityRequest = getEntityRequest(request)
      val entities = entityProcessor.getHighlightedText(entityRequest.text)
      Callback.successful(request.ok(entities, htmlHeaders))
    }

  }

  def getEntityRequest(request: HttpRequest): EntityRequest = {
    val inputParam = request.head.parameters.getFirst("input")
    val langParam =  request.head.parameters.getFirst("lang").getOrElse("")
    val requestAsStr: String = request.body.as[String].get
    val inputType = if (inputParam.equals("json")) "json" else "raw"

    try {
      if (inputType == "json") JsonUtil.fromJson[EntityRequest](requestAsStr)
      else new EntityRequest(requestAsStr, langParam)
    } catch {
      case _: Exception => null
    }
  }
}

class ServerInitializer(worker: WorkerRef) extends Initializer(worker) {

  def onConnect = context => new Server(context)

}


object Main extends App {
  implicit val system = ActorSystem()
  implicit val io = IOSystem()

  Server.start("language-server", 8002){ worker => new ServerInitializer(worker) }

}
