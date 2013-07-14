package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.shnappy.Env
import com.roundeights.skene._
import com.roundeights.scalon.nObject


/** Authentication handlers */
class AuthApiHandler(
    private val req: Registry,
    private val env: Env,
    private val data: AdminData,
    private val session: Session
) extends Skene {

    // A shared status code for a valid response
    private val ok = nObject("status" -> "ok").toString

    // Handle a login attempt
    put("/admin/api/login")(
        req.use[Persona[User]].in((prereqs, resp, recover) => {

            val cookie = Cookie(
                name = "auth",
                value = session.token( prereqs.user ),
                domain = Some(env.adminHost),
                secure = !env.adminDevMode,
                httpOnly = true
            )

            resp.cookie( cookie ).json(ok).done
        }
    ))

    put("/admin/api/logout")(req.use[Auth].in((prereqs, resp, recover) => {
        resp.cookie( Cookie("auth", "", ttl = None) ).json(ok).done
    }))

}


