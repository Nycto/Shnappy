package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.{Date, UUID}
import com.roundeights.hasher.{Algo, Hash}
import com.roundeights.skene.{Provider, Bundle, Registry, Cookie}
import com.roundeights.shnappy.Env
import com.roundeights.attempt._

/**
 * Parses and creates session tokens
 */
class Session (
    private val env: Env,
    private val getTime: () => Long
) {

    /** Defalt constructor */
    def this ( env: Env ) = this( env, () => new Date().getTime )

    // An alias for the secret key
    private val secret: String = env.secretKey

    /** Returns the HMAC seed needed for the given time offset */
    private def timeSeed ( offset: Int ): String = {
        assert( offset >= 0 )
        val time = getTime() / 1000
        val seed = time - (time % 60) - (60 * offset)
        Algo.hmac( secret ).sha512( seed.toString ).hex
    }

    /** HMACs the given value at the given time offset */
    def hmac ( value: String, offset: Int ): Hash
        = Algo.hmac( timeSeed(offset) + secret ).sha512( value )

    /** Generates a session token for the given user */
    def token ( email: String, user: User ): String = {
        val pair = email + "|" + user.id.toString
        pair + hmac( pair, 0 ).hex
    }

    /** Checks a token to see if it is valid */
    def checkToken ( token: String ): Option[(String, UUID)] = {
        val email = token.takeWhile( _ != '|' )
        val dataPair = token.take( email.length + 1 + 36 )
        val uuid = dataPair.drop(email.length + 1)

        val checksum = Hash( token.drop( dataPair.length ).take(128) )

        def checkOffset( offset: Int ): Option[(String, UUID)] = {
            if ( offset > 15 )
                None
            else if ( checksum == hmac(dataPair, offset) )
                Some( email -> UUID.fromString(uuid) )
            else
                checkOffset( offset + 1 )
        }

        checkOffset(0)
    }

    /** Builds a current session cookie for a user */
    def cookie ( email: String, user: User ): Cookie = Cookie(
        name = "auth",
        value = token( email, user ),
        domain = Some(env.adminHost),
        path = Some("/admin"),
        secure = !env.adminDevMode,
        httpOnly = true
    )

    /** Returns a cookie that will delete the auth token */
    def deleteCookie = Cookie(
        name = "auth",
        value = "",
        domain = Some(env.adminHost),
        path = Some("/admin"),
        ttl = Some(-1)
    )
}

/**
 * Represents the logged in state of a user
 */
trait Auth {

    /** The email address a user is authenticated with */
    def authEmail: String

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
    private def extractUser ( cookie: String ): Option[(String, UUID)] = {
        if ( live ) {
            session.checkToken( cookie )
        }
        else try {
            val email = cookie.takeWhile( _ != '|' )
            val userID = UUID.fromString( cookie.drop( email.length + 1 ) )
            Some( email -> userID )
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
                next.failure( new Unauthenticated(
                    "Auth cookie not found"
                ) )
            }

            // Pull the user ID out of the cookie
            (email, userID) <- extractUser( cookie.value ) :: OnFail {
                bundle.response.cookie( session.deleteCookie )
                next.failure( new Unauthenticated("Invalid auth cookie") )
            }

            // Fetch the user, if they exist
            userObj <- TryTo.lift {
                data.getUser( userID ) :: OnFail.alsoFail( next )
            } onFail {
                next.failure( new Unauthenticated("User does not exist") )
            }

        } {
            bundle.response.cookie( session.cookie( email, userObj ) )
            next.success( new Auth {
                override val authEmail = email
                override val user = userObj
            } )
        }
    }
}

