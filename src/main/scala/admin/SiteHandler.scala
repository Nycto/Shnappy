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
        req.use[SiteEditor, SiteParam].in((prereqs, resp, recover) => {
            resp.json( prereqs.siteParam.toJson.toString ).done
        })
    )

    // Updates specified values for a site
    patch("/admin/api/sites/:siteID")(
        req.use[SiteEditor, SiteParam, BodyData].in((prereqs, resp, recover)=>{
            for {
                updated <- TryTo.except {
                    prereqs.json.asObject.patch( prereqs.siteParam )
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
                    case err: Throwable => recover.orRethrow(err)
                }

                _ <- recover.fromFuture( data.save(updated) )

            } resp.json( updated.toJson.toString ).done
        })
    )
}

