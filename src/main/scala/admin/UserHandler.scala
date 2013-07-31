package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.Templater
import com.roundeights.scalon._
import com.roundeights.attempt._
import scala.concurrent.Promise
import java.util.UUID


/** Extracts a user being interacted with in the API */
trait UserEdit {

    /** The user being edited */
    def userEdit: User

    /** {@inheritDoc} */
    override def toString = "UserEdit(%s)".format( userEdit )
}

/**
 * Builds an UserEdit prereq
 */
class UserEditProvider( val data: AdminData ) extends Provider[UserEdit] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[UserEdit] ): Unit = {
        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("userID") )
            } onFailMatch {
                case _: Throwable =>
                    next.failure( new InvalidData("Invalid user ID") )
            }

            userOpt <- data.getUser(id) :: OnFail.alsoFail(next)

            user <- userOpt :: OnFail {
                throw new NotFound("User does not exist")
            }

        } next.success(new UserEdit {
            override val userEdit = user
        })
    }
}


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
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
            resp.json( prereqs.userEdit.toJson.toString ).done
        })
    )

    // Updates info for a specific user
    patch("/admin/api/users/:userID")(
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Deletes a user
    delete("/admin/api/users/:userID")(
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
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
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Grants a user access to a specific site
    put("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Revokes user access to a site
    delete("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserEdit].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )
}

