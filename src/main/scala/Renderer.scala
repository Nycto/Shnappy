package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}
import com.roundeights.tubeutil.Templater


/** A renderer is used to translate data into HTML */
class Renderer (
    private val engine: Templater,
    private val data: SiteData
) {

    /** Renders the given component type with the given data */
    def apply ( template: String, values: Map[String, Any] ): String
        = engine( data.siteInfo.theme + "/" + template, values )

    /** Renders the given component type with the given data */
    def apply ( template: String, values: (String, Any)* ): String
        = apply( template, Map(values:_*) )

    /** Renders the page level template */
    def renderPage
        ( content: String )
        ( implicit ctx: ExecutionContext )
    : Future[String] = {
        data.getNavLinks.map( links => apply("page",
            data.siteInfo.toMap +
            ("content" -> content) +
            ("nav" -> links.map( _.toMap ))
        ))
    }

}

