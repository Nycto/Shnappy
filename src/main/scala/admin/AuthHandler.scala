package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.roundeights.shnappy.Env
import com.roundeights.skene._
import com.roundeights.scalon.{nObject,nParser}
import com.roundeights.attempt._
import dispatch._


/** Authentication handlers */
class AuthApiHandler(
    private val req: Registry,
    private val env: Env,
    private val data: AdminData,
    private val session: Session
) extends Skene {

    /** Sends an auth request off to the person verification URL */
    private def persona ( email: String, assertion: String ): Future[Unit] = {
        val persona = dispatch.url(
            "https://verifier.login.persona.org/verify"
        )
        persona << Map(
            "assertion" -> assertion,
            "audience" -> "https://%s:443".format( env.adminHost )
        )
        Http( persona.OK(as.String) )
            .map( nParser.jsonObj _ )
            .map( obj => {
                if ( obj.str("status") != "okay" )
                    throw new Auth.VerificationFailed
                ()
            })
    }

    /** Verifyies an email and assertion */
    private def verify ( email: String, assertion: String ): Future[Unit] = {
        if ( env.adminDevMode )
            Future.successful( Unit )
        else
            persona( email, assertion )
    }

    /** Returns the cookie to set for a user */
    private def cookie ( user: User ) = Cookie(
        name = "auth",
        value = session.token( user ),
        domain = Some(env.adminHost),
        secure = !env.adminDevMode,
        httpOnly = true
    )

    // Handle a login attempt
    put("/admin/api/login")(req.use[BodyData].in((prereqs, resp, recover) => {
        val obj = prereqs.json.asObject

        // Extract and validate the pieces from the request
        for {
            assertion <- obj.str_?("assertion") :: OnFail {
                throw new BodyData.MissingKey("assertion")
            }

            rawEmail <- obj.str_?("email") :: OnFail {
                throw new BodyData.MissingKey("email")
            }

            email <- Some( User.email.process( rawEmail ).require.value )

            userOpt <- recover.fromFuture( data.getUserByEmail( email ) )

            user <- userOpt :: OnFail {
                throw new Auth.Unauthenticated("Unrecognized email: " + email)
            }

            _ <- recover.fromFuture( verify( email, assertion ) )
        } {
            resp.cookie( cookie(user) )
                .json( nObject("status" -> "ok").toString )
                .done
        }
    }))

    put("/admin/api/logout")(req.use[Auth].in((prereqs, resp, recover) => {
        resp.json( nObject("status" -> "ok").toString ).done
    }))

}


