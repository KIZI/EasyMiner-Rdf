package eu.easyminer.rdf

import java.io.ByteArrayInputStream

import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromStringUnmarshaller, Unmarshaller}
import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.data.RdfSource.JenaLang
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.rule.{RuleConstraint, Threshold}
import eu.easyminer.rdf.BasicExtractors.{AnyToDouble, AnyToInt}
import eu.easyminer.rdf.RdfMediaTypes._
import spray.json.DeserializationException

/**
  * Created by Vaclav Zeman on 8. 4. 2018.
  */
object TaskParamsUnmarshallers {

  implicit val datasetUnmarshaller: FromEntityUnmarshaller[Dataset] = {
    Unmarshaller.byteArrayUnmarshaller.forContentTypes(RdfMediaTypes.`application/n-triples`, RdfMediaTypes.`text/turtle`).mapWithInput { (entity, byteArray) =>
      Dataset(new ByteArrayInputStream(byteArray))(JenaLang(entity.contentType.mediaType.getLang))
    }
  }

  implicit val minHeadSizeUnmarshaller: FromStringUnmarshaller[Threshold.MinHeadSize] = Unmarshaller.strict {
    case AnyToInt(x) if x > 0 => Threshold.MinHeadSize(x)
    case _ => throw DeserializationException("Min head size must be integer and greater than zero.")
  }

  implicit val minHeadCoverageUnmarshaller: FromStringUnmarshaller[Threshold.MinHeadCoverage] = Unmarshaller.strict {
    case AnyToDouble(x) if x > 0 && x <= 1 => Threshold.MinHeadCoverage(x)
    case _ => throw DeserializationException("Min head coverage must be real number, greater than zero and lower than or equal to one.")
  }

  implicit val minConfidenceUnmarshaller: FromStringUnmarshaller[Threshold.MinConfidence] = Unmarshaller.strict {
    case AnyToDouble(x) if x > 0 && x <= 1 => Threshold.MinConfidence(x)
    case _ => throw DeserializationException("Min confidence must be real number, greater than zero and lower than or equal to one.")
  }

  implicit val maxRuleLengthUnmarshaller: FromStringUnmarshaller[Threshold.MaxRuleLength] = Unmarshaller.strict {
    case AnyToInt(x) if x > 1 && x <= 5 => Threshold.MaxRuleLength(x)
    case _ => throw DeserializationException("Max rule length must be integer, greater than one and lower than or equal to five.")
  }

  implicit val topKUnmarshaller: FromStringUnmarshaller[Threshold.TopK] = Unmarshaller.strict {
    case AnyToInt(x) if x > 1 => Threshold.TopK(x)
    case _ => throw DeserializationException("TopK value must be integer and greater than zero.")
  }

  implicit val withInstancesUnmarshaller: FromStringUnmarshaller[RuleConstraint.WithInstances] = Unmarshaller.strict {
    case "all" => RuleConstraint.WithInstances(false)
    case "objects" => RuleConstraint.WithInstances(true)
    case _ => throw DeserializationException("With instances constraint must have one of two possible values: 'all' or 'objects'.")
  }

  implicit val timeoutUnmarshaller: FromStringUnmarshaller[Threshold.Timeout] = Unmarshaller.strict {
    case AnyToInt(x) if x > 0 && x <= 30 => Threshold.Timeout(x)
    case _ => throw DeserializationException("Timeout must be integer, greater than zero and lower than or equal to 30.")
  }

}
