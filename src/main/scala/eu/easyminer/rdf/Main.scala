package eu.easyminer.rdf

import akka.actor.{ActorNotFound, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.rule.{RuleConstraint, Threshold}
import com.github.propi.rdfrules.utils.Debugger
import eu.easyminer.rdf.BasicExceptions.ValidationException
import eu.easyminer.rdf.TaskParamsUnmarshallers._
import eu.easyminer.rdf.TaskResultMarshallers._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 8. 4. 2018.
  */
object Main extends DefaultServer with DefaultServerConf {

  private implicit val timeout: Timeout = 10 seconds

  implicit val actorSystem: ActorSystem = ActorSystem("easyminer-rdf")
  implicit val materializer: Materializer = ActorMaterializer()

  lazy val maxTriples: Int = Conf[Int](configServerPrefix + ".max-triples").value

  val configServerPrefix: String = "easyminer.rdf"

  val route: Route = path(JavaUUID) { taskId =>
    post {
      onComplete(actorSystem.actorSelection("/user/" + taskId.toString).resolveOne()) {
        case Success(_) => reject(ValidationException("TaskIsAlreadyRunning", "Task has already been created and is running."))
        case Failure(_: ActorNotFound) =>
          toStrictEntity(30 seconds) {
            formFields(
              "name",
              "body".as[Dataset],
              "timeout".as[Threshold.Timeout].?(Threshold.Timeout(10)),
              "min-headsize".as[Threshold.MinHeadSize].?(Threshold.MinHeadSize(100)),
              "min-head-coverage".as[Threshold.MinHeadCoverage].?(Threshold.MinHeadCoverage(0.05)),
              "max-rule-length".as[Threshold.MaxRuleLength].?(Threshold.MaxRuleLength(3)),
              "min-confidence".as[Threshold.MinConfidence].?,
              "topk".as[Threshold.TopK].?,
              "instances".as[RuleConstraint.WithInstances].?,
              "duplicit-predicates".?
            ) { (name, dataset, timeout, minHeadSize, minHeadCoverage, maxRuleLength, minConfidence, topK, instances, duplicitPredicates) =>
              if (name == "amie") {
                actorSystem.actorOf(TaskActor.props(
                  taskId,
                  dataset.take(maxTriples),
                  logger => Debugger(logger) { implicit debugger =>
                    Function.chain[RulesMining](List(
                      x => minConfidence.foldLeft(x)(_ addThreshold _),
                      x => topK.foldLeft(x)(_ addThreshold _),
                      x => instances.foldLeft(x)(_ addConstraint _),
                      x => if (duplicitPredicates.isEmpty) x.addConstraint(RuleConstraint.WithoutDuplicitPredicates()) else x
                    ))(Amie(logger).addThreshold(timeout).addThreshold(minHeadSize).addThreshold(minHeadCoverage).addThreshold(maxRuleLength))
                  }
                ), taskId.toString)
                complete(StatusCodes.Accepted)
              } else {
                reject(ValidationException("AlgorithmIsNotSupported", "Name of algorithm is not supported. Applicable algortihms are: amie"))
              }
            }
          }
        case Failure(x) => throw x
      }
    } ~ get {
      onComplete(actorSystem.actorSelection("/user/" + taskId.toString).resolveOne()) {
        case Success(actor) => onComplete(actor ? TaskActor.Request.GetResult) {
          case Success(TaskActor.Response.InProgress(logs)) => complete(StatusCodes.Accepted, JsObject("logs" -> logs.toJson))
          case Success(TaskActor.Response.Error(th, _)) => throw th
          case Success(TaskActor.Response.Result(rules, logs)) => complete(StatusCodes.OK, JsObject("logs" -> logs.toJson, "rules" -> rules.toJson))
          case Failure(th) => throw th
          case _ => reject
        }
        case Failure(_: ActorNotFound) => reject
        case Failure(x) => throw x
      }
    }
  } ~ pathPrefix("test") {
    getFromDirectory("test")
  }

  def main(args: Array[String]): Unit = {
    bind()
  }

}
