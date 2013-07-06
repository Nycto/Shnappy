package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy._

/**
 * Utility endpoints
 */
class UtilEntry( env: Env ) extends Skene {

    // Load the static assets
    delegate( env.assets.handler )

    // Load the JS
    delegate( env.js.handler )

    // Load the CSS
    delegate( env.css.handler )

    // Wire up a handler for the favicon
    get("/favicon.ico")( env.assets.handler )

    // Handle a request to robots.txt
    get("/robots.txt")( _.text("User-agent: *\nAllow: /\n").done )

}

