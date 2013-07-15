package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry}
import com.roundeights.attempt._
import com.roundeights.scalon.{nParser, nObject}
import com.roundeights.vfunk.Validate
import dispatch._

/**
 * The result of a Persona verification request
 */
trait Persona {

    /** The email address */
    def email: String

    /** {@inheritDoc} */
    override def toString = "Persona(%s)".format( email )
}

/**
 * Executes a persona verification request
 */
class PersonaProvider (
    private val audience: String,
    private val live: Boolean
) extends Provider[Persona] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[BodyData] )

    // The URL to send verification requests to
    private val verifyURL = "https://verifier.login.persona.org/verify"

    /** Sends an auth request off to the person verification URL */
    private def verify ( assertion: String ): Future[nObject] = {
        if ( live ) {
            val request = dispatch.url( verifyURL )
            request << Map( "assertion" -> assertion, "audience" -> audience )
            Http( request.OK(as.String) ).map( nParser.jsonObj _ )
        }
        else {
            Future.successful(nObject("status" -> "ok", "email" -> assertion))
        }
    }

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[Persona] ): Unit = {
        val obj = bundle.get[BodyData].json.asObject

        for {
            assertion <- obj.str_?("assertion") :: OnFail {
                next.failure( new BodyData.MissingKey("assertion") )
            }

            json <- verify(assertion) :: OnFail.alsoFail( next )

            status <- json.str_?("status") :: OnFail {
                // @TODO: Customize this exception type
                next.failure( new Exception(
                    "Persona response missing status: %s".format( json )
                ))
            }

            _ <- ( status == "ok" ) :: OnFail {
                // @TODO: Customize this exception type
                next.failure( new Exception(
                    "Persona response status not ok: %s".format( json )
                ))
            }

            emailAddr <- json.str_?("email") :: OnFail {
                // @TODO: Customize this exception type
                next.failure( new Exception(
                    "Persona response missing email: %s: ".format( json )
                ))
            }

            _ <- TryTo.except {
                Validate.email.validate( emailAddr ).require
            } onFailMatch {
                // @TODO: Make this a user error
                case err: Throwable => next.failure( new Exception(err) )
            }

        } next.success( new Persona {
            override val email = emailAddr
        } )

    }
}



