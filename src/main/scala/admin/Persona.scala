package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry}
import com.roundeights.attempt._
import com.roundeights.scalon.nParser
import dispatch._

/**
 * The result of a Persona verification request
 */
trait Persona[T] {

    /** The email address */
    def email: String

    /** The user from the request */
    def user: T

    /** {@inheritDoc} */
    override def toString = "Persona(%s, %s)".format( email, user.toString )
}

/**
 * Executes a persona verification request
 */
class PersonaProvider[T] (
    private val audience: String,
    private val live: Boolean,
    private val getUser: (String) => Future[Option[T]]
) extends Provider[Persona[T]] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[BodyData] )

    // The URL to send verification requests to
    private val verifyURL = "https://verifier.login.persona.org/verify"

    /** Sends an auth request off to the person verification URL */
    private def verify (
        email: String, assertion: String
    ): Future[String] = {
        if ( live ) {
            val request = dispatch.url( verifyURL )
            request << Map( "assertion" -> assertion, "audience" -> audience )
            Http( request.OK(as.String) )
        }
        else {
            Future.successful("""{"status":"ok"}""")
        }
    }

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[Persona[T]] ): Unit = {
        val obj = bundle.get[BodyData].json.asObject

        for {
            assertion <- obj.str_?("assertion") :: OnFail {
                next.failure( new BodyData.MissingKey("assertion") )
            }

            rawEmail <- obj.str_?("email") :: OnFail {
                next.failure( new BodyData.MissingKey("email") )
            }

            emailAddr <- TryTo.except {
                User.email.process( rawEmail ).require.value
            } onFailMatch {
                // @TODO: Make this a user error
                case err: Throwable => next.failure( new Exception(err) )
            }

            userOpt <- getUser( emailAddr ) :: OnFail.alsoFail( next )

            userObj <- userOpt :: OnFail {
                // @TODO: Make this an authentication error
                next.failure( new Exception("User does not exist") )
            }

            response <- verify(emailAddr, assertion) :: OnFail.alsoFail( next )

            json <- TryTo.except { nParser.jsonObj( response ) } onFailMatch {
                // @TODO: Make this a user error
                case err: Throwable => next.failure( new Exception(err) )
            }

            status <- json.str_?("status") :: OnFail {
                // @TODO: Customize this exception type
                next.failure( new Exception(
                    "Invalid persona response: %s".format( response )
                ))
            }

            _ <- ( status == "ok" ) :: OnFail {
                // @TODO: Customize this exception type
                next.failure( new Exception(
                    "Persona response status not ok: %s".format( status )
                ))
            }

        } next.success( new Persona[T] {
            override val email = emailAddr
            override val user = userObj
        } )

    }
}



