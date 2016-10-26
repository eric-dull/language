import akka.actor.ActorSystem
import colossus._
import core._
import network.eic.language.Language
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

  val lang = Language
  val headers = HttpHeaders(
    HttpHeader("Content-Type", "application/json; charset=utf-8")
  )


  def handle = {
    case request @ Post on Root  => {
      var ll = request.head.parameters.getFirst("lang")
      val text: String = request.body.as[String].get // scala.io.Source.fromFile("resources/text.txt", "utf-8").getLines.mkString
      val entities = lang.getEntityPositionsByType(cleanText(text))
      Callback.successful(request.ok(JsonUtil.toJson(entities), headers))
    }

    case request @ Post on Root / "json" => {
      val requestAsStr: String = request.body.as[String].get // scala.io.Source.fromFile("resources/text.txt", "utf-8").getLines.mkString
      val entityRequest: EntityRequest = JsonUtil.fromJson[EntityRequest](requestAsStr)
      val entities = lang.getEntityPositionsByType(cleanText(entityRequest.text))
      Callback.successful(request.ok(JsonUtil.toJson(entities), headers))
    }

    case request @ Get on Root / "byType" => {
      val text: String = scala.io.Source.fromFile("resources/text.txt", "utf-8").getLines.mkString
      val entities = lang.getEntitiesByType(text)
      Callback.successful(request.ok(JsonUtil.toJson(entities), headers))
    }
  }

  def cleanText(text: String): String = {
    text.filter(_ >= ' ')
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
