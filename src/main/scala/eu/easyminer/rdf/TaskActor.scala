package eu.easyminer.rdf

import java.util.{Date, UUID}

import akka.actor.{Actor, Props, ReceiveTimeout}
import com.github.propi.rdfrules.algorithm.RulesMining
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.index.Index
import com.typesafe.scalalogging.Logger
import eu.easyminer.rdf.MappedRule._
import eu.easyminer.rdf.TaskActor.{Request, Response}
import org.slf4j.event.Level

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by Vaclav Zeman on 9. 4. 2018.
  */
class TaskActor private(taskId: UUID, dataset: Dataset, miner: Logger => RulesMining) extends Actor {

  context.setReceiveTimeout(1 hour)

  private implicit val ec: ExecutionContext = this.context.dispatcher

  private val msgBuffer = collection.mutable.ListBuffer.empty[(Date, String)]

  private val logger = CustomLogger(taskId.toString) { (msg, level) =>
    if (level == Level.INFO || level == Level.WARN || level == Level.ERROR) {
      msgBuffer.synchronized(msgBuffer += (new Date() -> msg))
    }
  }

  private val result = Future {
    logger.info("Dataset loading into memory...")
    val index = Index.fromDataset(dataset)
    val rules = index.tripleMap { implicit thi =>
      miner(logger).mine
    }
    logger.info(s"Mining task has been successful. Found rules: ${rules.size}.")
    index.tripleItemMap { implicit tihi =>
      rules.map(_.toMappedRule)
    }
  }

  def receive: Receive = {
    case Request.GetResult => result.value match {
      case Some(Success(rules)) =>
        context stop self
        sender() ! Response.Result(rules, msgBuffer.toList)
      case Some(Failure(x)) =>
        context stop self
        sender() ! Response.Error(x, msgBuffer.toList)
      case None => sender() ! Response.InProgress(msgBuffer.toList)
    }
    case ReceiveTimeout => context stop self
  }

}

object TaskActor {

  def props(taskId: UUID, dataset: Dataset, miner: Logger => RulesMining): Props = Props(new TaskActor(taskId, dataset, miner))

  sealed trait Response

  object Response {

    case class Result(rules: IndexedSeq[MappedRule], logs: List[(Date, String)])

    case class InProgress(logs: List[(Date, String)])

    case class Error(th: Throwable, logs: List[(Date, String)])

  }

  sealed trait Request

  object Request {

    case object GetResult extends Request

  }

}
