package com.roundeights.shnappy

import scala.concurrent.Future
import org.fusesource.scalate._
import java.io.File

/** A renderer is used to translate data into HTML */
class Renderer {

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
    def renderPage ( content: String ): Future[String] = {
        Future.successful( apply( "page", "content" -> content ) )
    }

}

