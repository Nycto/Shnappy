package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}
import org.fusesource.scalate._
import java.io.File

/** A renderer is used to translate data into HTML */
class Renderer ( private val env: Env, private val data: Data ) {

    /** The templating engine */
    private val engine = new TemplateEngine

    /** The root directory to look in for templates */
    val root = new File( env.rootDir, "templates" ).getAbsoluteFile

    /** Renders the given component type with the given data */
    def apply ( template: String, data: Map[String, Any] ): String = {
        val path = new File(root, template + ".mustache")
        engine.layout( path.toString, data )
    }

    /** Renders the given component type with the given data */
    def apply ( template: String, data: (String, Any)* ): String
        = apply( template, Map(data:_*) )

    /** Renders the page level template */
    def renderPage
        ( content: String )
        ( implicit ctx: ExecutionContext )
    : Future[String] = {
        val linksFuture = data.getNavLinks
        val infoFuture = data.getSiteInfo

        linksFuture.flatMap( links => infoFuture.map( info => {
            apply( "page",
                info.toMap +
                ("content" -> content) +
                ("nav" -> links.map( _.toMap ))
            )
        }))
    }

}

