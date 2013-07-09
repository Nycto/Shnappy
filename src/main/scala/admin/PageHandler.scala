package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._

/**
 * Page API handlers
 */
class PageApiHandler ( req: Registry ) extends Skene {

    post("/admin/pages")( req.use[Auth].in((prereqs, resp, recover) => {
        resp.text("ok").done
    }))

}

