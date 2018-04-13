package eu.easyminer.rdf

import java.util.Date

import spray.json._
import DefaultJsonProtocol._
import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.rule.Measure

/**
  * Created by Vaclav Zeman on 9. 4. 2018.
  */
object TaskResultMarshallers {

  implicit val logsJsonFormat: RootJsonFormat[(Date, String)] = new RootJsonFormat[(Date, String)] {
    def read(json: JsValue): (Date, String) = ???

    def write(obj: (Date, String)): JsValue = JsObject(
      "time" -> obj._1.toString.toJson,
      "msg" -> obj._2.toJson
    )
  }

  implicit val tripleItemJsonWriter: RootJsonWriter[TripleItem] = {
    case x: TripleItem.Uri => JsString(x.toString)
    case TripleItem.Text(x) => JsString(x)
    case TripleItem.NumberDouble(x) => JsNumber(x)
    case TripleItem.BooleanValue(x) => JsBoolean(x)
    case x: TripleItem => JsString(x.toString)
  }

  implicit val mappedAtomItemJsonWriter: RootJsonWriter[MappedRule.MappedAtomItem] = {
    case MappedRule.MappedAtomItem.Variable(v) => JsObject("type" -> JsString("variable"), "value" -> JsString(v.toString()))
    case MappedRule.MappedAtomItem.Constant(tripleItem) => JsObject("type" -> JsString("value"), "value" -> tripleItem.toJson)
  }

  implicit val mappedAtomJsonWriter: RootJsonWriter[MappedRule.MappedAtom] = (obj: MappedRule.MappedAtom) => JsObject(
    "subject" -> obj.subject.toJson,
    "predicate" -> obj.predicate.asInstanceOf[TripleItem].toJson,
    "object" -> obj.`object`.toJson
  )

  implicit val measureJsonWriter: RootJsonWriter[Measure] = {
    case Measure.BodySize(x) => JsObject("name" -> JsString("bodySize"), "value" -> JsNumber(x))
    case Measure.Confidence(x) => JsObject("name" -> JsString("confidence"), "value" -> JsNumber(x))
    case Measure.HeadConfidence(x) => JsObject("name" -> JsString("headConfidence"), "value" -> JsNumber(x))
    case Measure.HeadCoverage(x) => JsObject("name" -> JsString("headCoverage"), "value" -> JsNumber(x))
    case Measure.HeadSize(x) => JsObject("name" -> JsString("headSize"), "value" -> JsNumber(x))
    case Measure.Lift(x) => JsObject("name" -> JsString("lift"), "value" -> JsNumber(x))
    case Measure.PcaBodySize(x) => JsObject("name" -> JsString("pcaBodySize"), "value" -> JsNumber(x))
    case Measure.PcaConfidence(x) => JsObject("name" -> JsString("pcaConfidence"), "value" -> JsNumber(x))
    case Measure.PcaLift(x) => JsObject("name" -> JsString("pcaLift"), "value" -> JsNumber(x))
    case Measure.Support(x) => JsObject("name" -> JsString("support"), "value" -> JsNumber(x))
  }

  implicit val mappedRuleJsonFormat: RootJsonFormat[MappedRule] = new RootJsonFormat[MappedRule] {
    def read(json: JsValue): MappedRule = ???

    def write(obj: MappedRule): JsValue = JsObject(
      "text" -> JsString(obj.text),
      "head" -> obj.head.toJson,
      "body" -> JsArray(obj.body.iterator.map(_.toJson).toVector),
      "measures" -> JsArray(obj.measures.iterator.map(_.toJson).toVector)
    )
  }

}
