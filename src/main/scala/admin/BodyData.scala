package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry, QueryString}
import com.roundeights.scalon._


/** @see BodyData */
object BodyData {

    /** Thrown when the submitted data is invalid */
    class InvalidContent(
        message: String, cause: Throwable
    ) extends Exception(message) {
        def this(message: String) = this(message, null)
        def this(cause: Throwable) = this(null, cause)
    }
}

/**
 * Parsed content from the request body
 */
trait BodyData {

    /** The parsed data */
    def json: nObject

    /** {@inheritDoc} */
    override def toString = "BodyData(%s)".format( json.toString )
}

/**
 * Extracts the content from the request body
 */
class BodyDataProvider extends Provider[BodyData] {

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[BodyData] ): Unit = {
        val req = bundle.request

        try {
            val data = req.getContentType match {
                case Some("application/x-www-form-urlencoded")
                    => nElement( QueryString( req.bodyStr ).toMap ).asObject

                // @TODO: Check for the json content type
                case _ => nParser.json( req.bodyStr ).asObject
            }

            next.success(new BodyData { override val json = data })
        } catch {
            case err: nException => next.failure(
                new BodyData.InvalidContent("Invalid JSON: %s".format(
                    err.getMessage
                ))
            )
        }
    }
}


