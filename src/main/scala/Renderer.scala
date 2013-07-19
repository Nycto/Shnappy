package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}


/** A renderer is used to translate data into HTML */
class Renderer (
    private val engine: Templater,
    private val data: Data#Request
) {

    /** Renders the given component type with the given data */
    def apply ( template: String, data: (String, Any)* ): String
        = engine( template, Map(data:_*) )

    /** Renders the page level template */
    def renderPage
        ( content: String )
        ( implicit ctx: ExecutionContext )
    : Future[String] = {
        data.getNavLinks.map( links =>
            engine.apply( "page",
                data.siteInfo.toMap +
                ("content" -> content) +
                ("nav" -> links.map( _.toMap ))
            )
        )
    }

}

