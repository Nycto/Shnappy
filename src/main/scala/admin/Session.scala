package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.{Date, UUID}
import com.roundeights.hasher.{Algo, Hash}
import com.roundeights.skene.{Provider, Bundle, Registry}
import com.roundeights.attempt._

/**
 * Parses and creates session tokens
 */
class Session ( secret: String ) {

    /** Returns the HMAC seed needed for the given time offset */
    private def timeSeed ( offset: Int ): String = {
        assert( offset >= 0 )
        val time = new Date().getTime / 1000
        val seed = time - (time % 60) - (60 * offset)
        Algo.hmac( secret ).sha512( seed.toString ).hex
    }

    /** HMACs the given value at the given time offset */
    def hmac ( value: String, offset: Int ): Hash
        = Algo.hmac( timeSeed(offset) + secret ).sha512( value )

    /** Generates a session token for the given user */
    def token ( user: User ): String
        = user.id + hmac( user.id.toString, 0 ).hex

    /** Checks a token to see if it is valid */
    def checkToken ( token: String ): Option[UUID] = {
        val uuid = token.take(36)
        val checksum = Hash( token.drop(36).take(128) )

        def checkOffset( offset: Int ): Option[UUID] = {
            if ( offset > 15 )
                None
            else if ( checksum == hmac(uuid, offset) )
                Some( UUID.fromString(uuid) )
            else
                checkOffset( offset + 1 )
        }

        checkOffset(0)
    }
}

/** @see Auth */
object Auth {

    /** Thrown when a user isn't logged in */
    class Unauthenticated( message: String ) extends Exception(message)

    /** Thrown when a user fails authentication */
    class Unauthorized( message: String ) extends Exception(message)

    /** Thrown when a page isn't loaded via HTTPs */
    class Insecure extends Exception

    /** Thrown when a user isn't logged in */
    class VerificationFailed extends Exception
}

/**
 * Represents the logged in state of a user
 */
trait Auth {

    /** The user from the request */
    def user: User

    /** {@inheritDoc} */
    override def toString = user.toString
}

/**
 * Builds a logged in user
 */
class AuthProvider (
    private val live: Boolean,
    private val session: Session,
    private val data: AdminData
) extends Provider[Auth] {

    /** Extracts the user ID from a token */
    private def extractUserID ( cookie: String ): Option[UUID] = {
        if ( live ) {
            session.checkToken( cookie )
        }
        else try {
            Some( UUID.fromString(cookie) )
        }
        catch {
            case _: IllegalArgumentException => session.checkToken( cookie )
        }
    }

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[Auth] ): Unit = {
        for {

            // Extract the auth cookie
            cookie <- bundle.request.cookies.first("auth") :: OnFail {
                next.failure( new Auth.Unauthenticated(
                    "Auth cookie not found"
                ) )
            }

            // Pull the user ID out of the cookie
            userID <- extractUserID( cookie.value ) :: OnFail {
                next.failure( new Auth.Unauthenticated("Invalid auth cookie") )
            }

            // Fetch the user, if they exist
            userOpt <- data.getUser( userID ) :: OnFail.alsoFail( next )

            // Extract the user from the option
            userObj <- userOpt :: OnFail {
                next.failure( new Auth.Unauthenticated("User does not exist") )
            }

        } next.success( new Auth {
            override val user = userObj
        } )
    }
}

