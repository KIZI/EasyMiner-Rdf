package eu.easyminer.rdf

import akka.http.scaladsl.server.Rejection

/**
  * Created by Vaclav Zeman on 8. 4. 2018.
  */
object BasicExceptions {

  case class ValidationException(code: String, msg: String) extends Exception(msg) with Rejection

  object ValidationException {

    val InvalidInputData = ValidationException("InvalidInputData", "Invalid input data.")

  }

}
