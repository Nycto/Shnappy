package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject


/** Authentication handlers */
class AuthApiHandler(
    private val req: Registry,
    private val data: AdminData,
    private val session: Session
) extends Skene {

    // A shared status code for a valid response
    private val ok = nObject("status" -> "ok").toString

    // Handle a login attempt
    put("/admin/api/login")(
        req.use[Persona].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getUserByEmail( prereqs.email )
            ).onSuccess {
                case None => recover.orRethrow(
                    new Unauthorized("User does not exist")
                )
                case Some(user) => {
                    resp.cookie( session.cookie(prereqs.email, user) )
                        .json(ok).done
                }
            }
        }
    ))

    put("/admin/api/logout")( _.cookie( session.deleteCookie ).json(ok).done )

}


