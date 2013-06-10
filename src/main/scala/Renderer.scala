package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}
import org.fusesource.scalate._
import java.io.File

/** A renderer is used to translate data into HTML */
class Renderer ( private val data: Data ) {

    /** The templating engine */
    private val engine = new TemplateEngine

    /** The root directory to look in for templates */
    val root = new File( System.getProperty("user.dir") + "/templates" )

    /** Renders the given component type with the given data */
    def apply ( template: String, data: (String, Any)* ): String = {
        val path = new File(root, template + ".mustache")
        engine.layout( path.toString, Map(data:_*) )
    }

    /** Renders the page level template */
    def renderPage
        ( content: String )
        ( implicit ctx: ExecutionContext )
    : Future[String] = {
        data.getNav.map( nav => apply( "page",
            "content" -> content,
            "nav" -> nav.map( _.toMap )
        ))
    }

}

