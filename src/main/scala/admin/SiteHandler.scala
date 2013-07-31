package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.shnappy._
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._

/**
 * Site API handlers
 */
class SiteApiHandler ( val req: Registry, val data: AdminData ) extends Skene {

    // Return a list of all sites
    get("/admin/api/sites")(
        req.use[Auth].in((prereqs, resp, recover) => {
            recover.fromFuture( data.getSites ).onSuccess { case sites => {
                val user = prereqs.user
                resp.json( nElement( sites.filter(
                    site => user.canChange(site.id)
                )).toString ).done
            }}
        })
    )

    // Create a new site
    post("/admin/api/sites")(
        req.use[Admin, BodyData].in((prereqs, resp, recover) => {
            for {

                site <- TryTo.except {
                    val json = prereqs.json.asObject
                    SiteInfo(
                        json.str("theme"),
                        json.str("title"),
                        json.str_?("favicon"),
                        if ( json.contains("host") )
                            Set( json.str("host") )
                        else
                            json.ary("hosts").map( _.asString ).toSet
                    )

                } onFailMatch {
                    case err: nException => recover.orRethrow(
                        new InvalidData( err.getMessage )
                    )
                    case err: Throwable => recover.orRethrow( err )
                }

                _ <- recover.fromFuture( data.save(site) )

            } resp.json( site.toJson.toString ).done
        })
    )

    // Returns the details about a specific site
    get("/admin/api/sites/:siteID")(
        req.use[SiteEditor].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getSite( prereqs.siteID )
            ).onSuccess {
                case None => throw new NotFound("Site not be found")
                case Some(site) => resp.json( site.toJson.toString ).done
            }
        })
    )

    // Updates specified values for a site
    patch("/admin/api/sites/:siteID")(
        req.use[SiteEditor, BodyData].in((prereqs, resp, recover) => {
            for {
                siteOpt <- recover.fromFuture( data.getSite(prereqs.siteID) )

                site <- siteOpt :: OnFail {
                    recover.orRethrow( new NotFound("Site not be found") )
                }

                updated <- TryTo.except {
                    prereqs.json.asObject.patch(site)
                        .patch[String]("theme", _ withTheme _)
                        .patch[String]("title", _ withTitle _)
                        .done
                } onFailMatch {
                    case err: Throwable => recover.orRethrow(err)
                }

                _ <- recover.fromFuture( data.save(updated) )

            } resp.json( updated.toJson.toString ).done
        })
    )
}

