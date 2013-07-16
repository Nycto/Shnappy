package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry}
import com.roundeights.attempt._
import com.roundeights.scalon.{nParser, nObject, nTypeMismatch}
import com.roundeights.vfunk.{Validate, InvalidValueException}
import dispatch._

/** @see Persona */
object Persona {

    /** General Persona errors */
    class Error( message: String ) extends Exception( message )
}

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

    // Validates an email address
    private val emailValid = Validate.email

    // The URL to send verification requests to
    private val verifyURL = "https://verifier.login.persona.org/verify"

    /** Sends an auth request off to the person verification URL */
    private def verify ( assertion: String ): Future[nObject] = {
        if ( live || !emailValid.validate(assertion).isValid ) {
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

        for {

            obj <- TryTo.except {
                bundle.get[BodyData].json.asObject
            } onFailMatch {
                case err: nTypeMismatch => next.failure(
                    new BodyData.InvalidContent(err)
                )
            }

            assertion <- obj.str_?("assertion") :: OnFail {
                next.failure( new BodyData.MissingKey("assertion") )
            }

            json <- verify(assertion) :: OnFail.alsoFail( next )

            status <- json.str_?("status") :: OnFail {
                next.failure( new Persona.Error(
                    "Persona response missing status: %s".format( json )
                ))
            }

            _ <- ( status == "ok" ) :: OnFail {
                next.failure( new Auth.Unauthorized(
                    "Persona response status not ok: %s".format( json )
                ))
            }

            emailAddr <- json.str_?("email") :: OnFail {
                next.failure( new Persona.Error(
                    "Persona response missing email: %s: ".format( json )
                ))
            }

            _ <- TryTo.except {
                emailValid.validate( emailAddr ).require
            } onFailMatch {
                case err: InvalidValueException => next.failure(
                    new Persona.Error(
                        "Invalid Email returned from Persona: %s, %s".format(
                            emailAddr, err.firstError
                        )
                    )
                )
            }

        } next.success( new Persona {
            override val email = emailAddr
        } )

    }
}



