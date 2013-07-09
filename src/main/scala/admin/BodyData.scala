package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry, QueryString}
import com.roundeights.scalon._


/**
 * Parsed content from the request body
 */
trait BodyData {

    /** The parsed data */
    def json: nElement

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

        // @TODO: Wrap this in a try catch to make sure parse errors
        // are a client error
        val data = req.headers.get("Content-Type") match {
            case Some("application/x-www-form-urlencoded")
                => nElement( QueryString( req.bodyStr ).toMap )

            // @TODO: Check for the json content type
            case _ => nParser.jsonObj( req.bodyStr )
        }

        println( data.toString )

        next.success(new BodyData { override val json = data })
    }
}


