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

    get("/admin") {
        req.use[Auth, AdminTemplate].in((prereqs, resp, recover) => {
            if ( prereqs.user.sites.size > 1 || prereqs.user.isAdmin ) {
                resp.html( prereqs.template("admin/pages/sites") ).done
            }
            else if ( prereqs.user.sites.size == 0 ) {
                throw new Unauthorized("User can not edit any sites")
            }
            else {
                resp.moved("/admin/sites/%s/content".format(
                    prereqs.user.sites.head.toString
                )).done
            }
        })
    }

    get("/admin/users").or.get("/admin/users/*") {
        req.use[Admin, AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/users") ).done
        })
    }

    get("/admin/sites").or.get("/admin/sites/*") {
        req.use[AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/sites") ).done
        })
    }

    get("/admin/sites/*/content") {
        req.use[AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/content") ).done
        })
    }
}
