package com.roundeights.shnappy.handler

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.shnappy._

/**
 * Utility endpoints
 */
class UtilEntry extends Skene {

    /** A shared renderer */
    private val renderer = new Renderer( Env.env, Data() )

    // Load the static assets
    delegate( Env.env.assets.handler )

    // Load the JS
    delegate( Env.env.js.handler )

    // Load the CSS
    delegate( Env.env.css.handler )

    // Wire up a handler for the favicon
    get("/favicon.ico")( Env.env.assets.handler )

    // Handle a request to robots.txt
    get("/robots.txt")( _.text("User-agent: *\nAllow: /\n").done )

}

