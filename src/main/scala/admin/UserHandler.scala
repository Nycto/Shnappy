package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._
import java.util.UUID


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
        req.use[Admin, BodyData].in((prereqs, resp, recover) => {
            val json = prereqs.json

            for {

                user <- TryTo.except {
                    User(
                        json.str("name"),
                        json.str("email"),
                        if ( json.contains("site") ) {
                            Set( json.uuid("site") )
                        }
                        else {
                            json.ary("sites").map( _.asUUID ).toSet
                        },
                        json.bool_?("isAdmin").getOrElse( false )
                    )

                } onFailMatch {
                    case err: nException => recover.orRethrow(
                        new InvalidData( err.getMessage )
                    )
                    case err: Throwable => recover.orRethrow( err )
                }

                _ <- recover.fromFuture( data.save(user) )

            } resp.json( user.toJson.toString ).done
        })
    )

    // Returns details for a specific user
    get("/admin/api/users/:userID")(
        req.use[Admin, UserParam].in((prereqs, resp, recover) => {
            resp.json( prereqs.userParam.toJson.toString ).done
        })
    )

    // Updates info for a specific user
    patch("/admin/api/users/:userID")(
        req.use[Admin, UserParam, BodyData].in((prereqs, resp, recover) => {

            for {
                updated <- TryTo.except {
                    prereqs.json.patch( prereqs.userParam )
                        .patch[String]("name", _ withName _)
                        .patch[String]("email", _ withEmail _)
                        .patch[Boolean]("isAdmin", _ setAdmin _)
                        .patch[nList]("sites", (user, sites) => {
                            user.setSites( sites.map(_.asUUID).toSet )
                        })
                        .done
                } onFailMatch {
                    case err: Throwable => recover.orRethrow(err)
                }

                _ <- recover.fromFuture( data.save(updated) )

            } resp.json( updated.toJson.toString ).done
        })
    )

    // Deletes a user
    delete("/admin/api/users/:userID")(
        req.use[Admin, UserParam].in((prereqs, resp, recover) => {
            recover.fromFuture( data.delete( prereqs.userParam ) ).onSuccess {
                case _ => resp.json( nObject("status" -> "ok").toString ).done
            }
        })
    )

    // Returns the sites a user has access to
    get("/admin/api/users/:userID/sites")(
        req.use[Admin, UserParam].in((prereqs, resp, recover) => {
            resp.json( nElement(
                prereqs.userParam.sites.map(_.toString)
            ).toString ).done
        })
    )

    // Grants a user access to a specific site
    put("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserParam, SiteParam].in((prereqs, resp, recover) => {

            val newUser = prereqs.userParam.addSite(
                prereqs.siteParam.id
            )

            recover.fromFuture( data.save(newUser) ).onSuccess {
                case _ => resp.json( newUser.toJson.toString ).done
            }
        })
    )

    // Revokes user access to a site
    delete("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserParam, SiteParam].in((prereqs, resp, recover) => {

            val newUser = prereqs.userParam.removeSite(
                prereqs.siteParam.id
            )

            recover.fromFuture( data.save(newUser) ).onSuccess {
                case _ => resp.json( newUser.toJson.toString ).done
            }
        })
    )
}

