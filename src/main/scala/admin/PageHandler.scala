package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.Templater

/**
 * Page API handlers
 */
class PageApiHandler ( val req: Registry ) extends Skene {

    post("/admin/api/pages")( req.use[Auth].in((prereqs, resp, recover) => {
        resp.text("ok").done
    }))
}

/**
 * Page HTML handler
 */
class PageHtmlHandler (
    val template: Templater, val req: Registry
) extends Skene {

    get("/admin/pages")( req.use[Auth].in((prereqs, resp, recover) => {
        resp.html("ok").done
    }))
}

