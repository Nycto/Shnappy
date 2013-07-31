package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.Templater
import com.roundeights.scalon._

/**
 * User API handlers
 */
class UserApiHandler ( val req: Registry, val data: AdminData ) extends Skene {

    // Returns all the users in the system
    get("/admin/api/users")(
        req.use[Admin].in((prereqs, resp, recover) => {
            recover.fromFuture( data.getUsers ).onSuccess { case users => {
                resp.json( nElement(users).toString ).done
            }}
        })
    )

    // Creates a new user
    post("/admin/api/users")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Returns details for a specific user
    get("/admin/api/users/:userID")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Updates info for a specific user
    patch("/admin/api/users/:userID")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Deletes a user
    delete("/admin/api/users/:userID")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Returns all the users that have access to a specific site
    get("/admin/api/sites/:siteID/users")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Returns the sites a user has access to
    get("/admin/api/users/:userID/sites")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Grants a user access to a specific site
    put("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Revokes user access to a site
    delete("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )
}

