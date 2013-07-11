package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.shnappy.Env
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.attempt._


/** Authentication handlers */
class AuthApiHandler(
    private val req: Registry,
    private val env: Env,
    private val data: AdminData,
    private val session: Session
) extends Skene {

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
        } {
            val token = session.token( user )

            val cookie = Cookie(
                name = "auth",
                value = token,
                domain = Some(env.adminHost),
                secure = !env.adminDevMode,
                httpOnly = true
            )

            if ( env.adminDevMode ) {
                resp.cookie(cookie)
                    .json( nObject("status" -> "ok").toString )
                    .done
            }
            else {
                resp.serverError.text("unimplemented").done
            }
        }
    }))

    put("/admin/api/logout")(req.use[Auth].in((prereqs, resp, recover) => {
        resp.json( nObject("status" -> "ok").toString ).done
    }))

}


