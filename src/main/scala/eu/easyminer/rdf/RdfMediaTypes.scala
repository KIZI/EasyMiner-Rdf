package eu.easyminer.rdf

import akka.http.scaladsl.model.{HttpCharsets, MediaType}
import org.apache.jena.riot.RDFFormat

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 8. 4. 2018.
  */
object RdfMediaTypes {

  implicit def mediaTypeToJenaFormat(mediaType: MediaType): RDFFormat = {
    mediaType match {
      case RdfMediaTypes.`application/n-triples` => RDFFormat.NTRIPLES
      case RdfMediaTypes.`text/turtle` => RDFFormat.TURTLE
      case _ => throw new IllegalArgumentException(s"Media type '${mediaType.value}' is not a valid RDF type.")
    }
  }

  val `application/n-triples`: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("n-triples", HttpCharsets.`UTF-8`, "nt")

  val `text/turtle`: MediaType.WithFixedCharset = MediaType.textWithFixedCharset("turtle", HttpCharsets.`UTF-8`, "ttl")

}
