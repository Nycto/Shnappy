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

    get("/admin/pages")(
        req.use[Auth, AdminTemplate].in((prereqs, resp, recover) => {
            resp.html( prereqs.template("admin/pages/pages") ).done
        })
    )
}
