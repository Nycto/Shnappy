package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.Templater
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
            for {

                user <- TryTo.except {
                    val json = prereqs.json.asObject
                    User(
                        json.str("name"),
                        json.str("email"),
                        if ( json.contains("site") ) {
                            Set( UUID.fromString( json.str("site") ) )
                        }
                        else {
                            json.ary("sites").map(
                                siteID => UUID.fromString( siteID.asString )
                            ).toSet
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
                    prereqs.json.asObject.patch( prereqs.userParam )
                        .patch[String]("name", _ withName _)
                        .patch[String]("email", _ withEmail _)
                        .patch[Boolean]("isAdmin", _ setAdmin _)
                        .patch[nList]("sites", (user, sites) => {
                            user.setSites( sites.map(
                                siteID => UUID.fromString( siteID.toString )
                            ).toSet )
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

    // Returns all the users that have access to a specific site
    get("/admin/api/sites/:siteID/users")(
        req.use[Admin].in((prereqs, resp, recover) => {
            resp.text("ok").done
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

    /** Safely extracts the siteID from a prereq object */
    private def extractSiteID ( recover: Recover, prereq: Prereq ) = {
        TryTo.except {
            UUID.fromString( prereq.request.params("siteID") )
        } onFailMatch {
            case _: Throwable => recover.orRethrow(
                new InvalidData("Invalid Site ID")
            )
        }
    }

    // Grants a user access to a specific site
    put("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserParam].in((prereqs, resp, recover) => {
            for {
                siteID <- extractSiteID(recover, prereqs)

                siteOpt <- recover.fromFuture( data.getSite(siteID) )

                _ <- siteOpt :: OnFail {
                    recover.orRethrow( new InvalidData("Site does not exist") )
                }

                newUser <- Some( prereqs.userParam.addSite(siteID) )

                _ <- recover.fromFuture( data.save(newUser) )

            } resp.json( newUser.toJson.toString ).done
        })
    )

    // Revokes user access to a site
    delete("/admin/api/users/:userID/sites/:siteID")(
        req.use[Admin, UserParam].in((prereqs, resp, recover) => {
            for {
                siteID <- extractSiteID(recover, prereqs)

                newUser <- Some( prereqs.userParam.removeSite(siteID) )

                _ <- recover.fromFuture( data.save(newUser) )

            } resp.json( newUser.toJson.toString ).done
        })
    )
}

