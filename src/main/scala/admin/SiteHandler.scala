package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.shnappy._
import com.roundeights.skene._
import com.roundeights.tubeutil.BodyData
import com.roundeights.scalon._
import com.roundeights.attempt._
import com.roundeights.vfunk.InvalidValueException

/**
 * Site API handlers
 */
class SiteApiHandler ( val req: Registry, val data: AdminData ) extends Skene {

    // Return a list of all sites
    get("/admin/api/sites")(
        req.use[Auth].in((prereqs, resp, recover) => {
            recover.fromFuture( data.getSites ).onSuccess { case sites => {
                val user = prereqs.user
                resp.json( nElement(
                    sites.filter( site => user.canChange(site.id) )
                        .map( _.toJsonLite )
                ).toString ).done
            }}
        })
    )

    // Create a new site
    post("/admin/api/sites")(
        req.use[Admin, BodyData].in((prereqs, resp, recover) => {
            val json = prereqs.json

            for {

                site <- TryTo.except {
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
                    case err@( _:nException | _:InvalidValueException ) =>
                        recover.orRethrow( new InvalidData( err ) )
                    case err: Throwable => recover.orRethrow( err )
                }

                _ <- recover.fromFuture( data.save(site) )

            } resp.json( site.toJson.toString ).done
        })
    )

    // Returns the details about a specific site
    get("/admin/api/sites/:siteID")(
        req.use[SiteEditor, SiteParam].in((prereqs, resp, recover) => {
            resp.json( prereqs.siteParam.toJson.toString ).done
        })
    )

    // Updates specified values for a site
    patch("/admin/api/sites/:siteID")(
        req.use[
            SiteEditor, SiteParam, BodyData
        ].in((prereqs, resp, recover) => {

            for {
                updated <- TryTo.except {
                    prereqs.json.patch( prereqs.siteParam )
                        .patch[String]("theme", _ withTheme _)
                        .patchElem("title", (site, title) => {
                            site.withTitle( title.asString_? )
                        })
                        .patchElem("favicon", (site, favicon) => {
                            site.withFavicon( favicon.asString_? )
                        })
                        .patch[String]("host", (site, host) => {
                            site.withHosts( Set(host) )
                        })
                        .patch[nList]("hosts", (site, hosts) => {
                            site.withHosts( hosts.map( _.asString ).toSet )
                        })
                        .done
                } onFailMatch {
                    case err@( _:nException | _:InvalidValueException ) =>
                        recover.orRethrow( new InvalidData( err ) )
                    case err: Throwable => recover.orRethrow(err)
                }

                _ <- recover.fromFuture( data.save(updated) )

            } resp.json( updated.toJson.toString ).done
        })
    )

    // Returns the details about a specific site
    delete("/admin/api/sites/:siteID")(
        req.use[Admin, SiteParam].in((prereqs, resp, recover) => {
            recover.fromFuture(
                // We are only deleting the site info. This is enough to take
                // it out of commission, but allows data to be recovered if
                // a site is deleted by mistake
                data.getSite( prereqs.siteParam.id )
                    .map( _.map( site => data.delete( site ) ) )
            ).onSuccess {
                case _ => resp.json( nObject("status" -> "ok").toString ).done
            }
        })
    )

    // Returns all the users that have access to a specific site
    get("/admin/api/sites/:siteID/users")(
        req.use[Admin, SiteParam].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getUsersBySiteID( prereqs.siteParam.id )
            ).onSuccess { case users => {
                resp.json( nElement(users).toString ).done
            }}
        })
    )
}

