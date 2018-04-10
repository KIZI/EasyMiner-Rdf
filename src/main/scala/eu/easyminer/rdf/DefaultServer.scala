package eu.easyminer.rdf

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import eu.easyminer.rdf.BasicExceptions.ValidationException
import spray.json.{DeserializationException, JsObject, JsString}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Promise}
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
trait DefaultServer {

  private val logger = Logger[DefaultServer]

  implicit val actorSystem: ActorSystem
  implicit val materializer: Materializer

  val host: String
  val port: Int
  val rootPath: String

  val route: Route

  private val bindingPromise = Promise[Http.ServerBinding]()
  private implicit lazy val ec: ExecutionContext = actorSystem.dispatcher

  final lazy val rootRoute: Route = {
    val rejectionHandler = RejectionHandler.newBuilder().handle {
      case ValidationException(code, msg) => completeErrorMessage(code, msg)
    }.result().withFallback(RejectionHandler.default.mapRejectionResponse {
      case res@HttpResponse(status, _, ent: HttpEntity.Strict, _) =>
        val message = ent.data.utf8String.replaceAll("\"", """\"""")
        res.copy(entity = HttpEntity(ContentTypes.`application/json`, s"""{ "code": "${status.value}", "message": "$message"}"""))
      case x => x
    })
    val exceptionHandler = ExceptionHandler {
      case ValidationException(code, msg) => completeErrorMessage(code, msg)
      case DeserializationException(msg, _, _) => completeErrorMessage(ValidationException.InvalidInputData.code, msg)
      case th =>
        logger.error(th.getMessage, th)
        completeErrorMessage(th.getClass.getSimpleName, th.getMessage, 500)
    }
    decodeRequest {
      encodeResponse {
        respondWithHeader(`Access-Control-Allow-Origin`.*) {
          mapResponseEntity(jsonToUtf8JsonEntity) {
            handleRejections(rejectionHandler) {
              handleExceptions(exceptionHandler) {
                pathPrefix(rootPath) {
                  cancelRejections(classOf[MethodRejection]) {
                    options {
                      extractRequest { request =>
                        respondWithHeaders(
                          List(
                            request.header[`Access-Control-Request-Headers`].map(x => `Access-Control-Allow-Headers`(x.headers)),
                            request.header[`Access-Control-Request-Method`].map(x => `Access-Control-Allow-Methods`(x.method)),
                            Some(`Access-Control-Allow-Credentials`(true))
                          ).flatten
                        ) {
                          complete("")
                        }
                      }
                    }
                  } ~ route ~ path("stop-1144") {
                    actorSystem.scheduler.scheduleOnce(2 seconds) {
                      bindingPromise.future.foreach { serverBinding =>
                        serverBinding.unbind().onComplete(_ => actorSystem.terminate())
                      }
                    }
                    complete("Stopping...")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private def jsonToUtf8JsonEntity(responseEntity: ResponseEntity): ResponseEntity = if (responseEntity.contentType.mediaType == MediaTypes.`application/json`) {
    responseEntity.withContentType(ContentType.WithCharset(MediaType.applicationWithOpenCharset("json"), HttpCharsets.`UTF-8`))
  } else {
    responseEntity
  }

  private def completeErrorMessage(code: String, msg: String, status: Int = 400) = complete(status, JsObject("code" -> JsString(code), "message" -> JsString(msg)))

  def bind(): Unit = {
    val bindingFuture = Http().bindAndHandle(RouteResult.route2HandlerFlow(rootRoute), host, port)
    bindingFuture.foreach { serverBinding =>
      bindingPromise.success(serverBinding)
      println(s"Server online at http://$host:$port/$rootPath")
    }
  }

}