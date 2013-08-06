package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy._

/**
 * Page HTML handler
 */
class HtmlHandler (
    val template: Templater, val req: Registry
) extends Skene {

    get("/admin")(
        req.use[Auth, AdminTemplate].in((prereqs, resp, recover) => {
            if ( prereqs.user.sites.size > 1 || prereqs.user.isAdmin ) {
                resp.html( prereqs.template("admin/pages/sites") ).done
            }
            else {
                resp.moved("/admin/site/%s".format(
                    prereqs.user.sites.head.toString
                )).done
            }
        })
    )

    get("/admin/users")(
        req.use[Admin, AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/users") ).done
        })
    )

    get("/admin/sites/:siteID")(
        req.use[SiteEditor, AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/edit") ).done
        })
    )
}
