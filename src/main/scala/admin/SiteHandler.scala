package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._

/**
 * Site API handlers
 */
class SiteApiHandler ( val req: Registry, val data: AdminData ) extends Skene {

    // Return a list of all sites
    get("/admin/api/sites")(
        req.use[Auth].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Create a new site
    post("/admin/api/sites")(
        req.use[Auth].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Returns the details about a specific site
    get("/admin/api/sites/:siteID")(
        req.use[Auth].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )

    // Updates specified values for a site
    patch("/admin/api/sites/:siteID")(
        req.use[Auth].in((prereqs, resp, recover) => {
            resp.text("ok").done
        })
    )
}

