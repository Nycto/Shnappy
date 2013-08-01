package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.shnappy.Templater
import java.util.UUID

/**
 * Page API handlers
 */
class ContentApiHandler (
    val req: Registry, val data: AdminData
) extends Skene {

    // Returns all the content for a specific site
    get("/admin/api/sites/:siteID/content")(
        req.use[SiteEditor].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getPagesAndLinks( prereqs.siteID )
            ).onSuccess {
                case content => resp.json( nElement( content.map {
                    case Left(page) => page.toJson
                    case Right(link) => link.toJson
                } ).toString ).done
            }
        })
    )

    // Creates a new piece of content
    post("/admin/api/sites/:siteID/content")(
        req.use[SiteEditor, SiteParam].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Returns a specific piece of content
    get("/admin/api/content/:contentID")(
        req.use[Auth, ContentParam].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Updates a specific piece of content
    patch("/admin/api/content/:contentID")(
        req.use[Auth, ContentParam].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Deletes a specific piece of content
    delete("/admin/api/content/:contentID")(
        req.use[Auth, ContentParam].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )
}

/**
 * Page HTML handler
 */
class ContentHtmlHandler (
    val template: Templater, val req: Registry
) extends Skene {

    get("/admin/pages")(
        req.use[Auth, AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/pages") ).done
        })
    )
}

