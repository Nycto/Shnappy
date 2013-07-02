package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy.admin.Prereq.require.use

/**
 * Page API handlers
 */
class PageHandler extends Skene {

    post("/admin/pages")( use[Auth].in((prereqs, resp, recover) => {
        resp.text("ok").done
    }))

}

