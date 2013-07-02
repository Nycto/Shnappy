package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy._
import com.roundeights.shnappy.admin.Prereq.require.use

/**
 * Admin handlers
 */
class AdminHandler extends Skene {

    post("/admin/pages")( use[Auth].in((prereqs, resp, recover) => {
        resp.text("ok").done
    }))

}

