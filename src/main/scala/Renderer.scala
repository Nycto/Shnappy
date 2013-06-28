package com.roundeights.shnappy

import scala.concurrent.{Future, ExecutionContext}
import scala.collection.JavaConversions

import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.{Handlebars, Context, Helper, Options}
import java.io.File
import java.util.{HashMap => JavaMap}


/** A renderer is used to translate data into HTML */
class Renderer ( private val env: Env, private val data: Data ) {

    /** Templating engine */
    private val engine = new Handlebars(
        new FileTemplateLoader(
            new File( env.rootDir, "templates" ).getAbsoluteFile,
            ".hbs"
        )
    )

    /** Turns a value into a list of strings */
    private def extractValues ( opts: Options ): Seq[String]
        = opts.fn().toString.split(",").map( _.trim ).filter( _ != "" )

    // Add a helper to load the JavaScript
    engine.registerHelper( "js", new Helper[Any] {
        /** {@inheritDoc} */
        override def apply( value: Any, opts: Options )
            = env.loader.js( extractValues( opts ):_* )
    });

    // Add a helper to load the CSS
    engine.registerHelper( "css", new Helper[Any] {
        /** {@inheritDoc} */
        override def apply( value: Any, opts: Options )
            = env.loader.css( extractValues( opts ):_* )
    });

    // Add a helper to load the URL for a single asset
    engine.registerHelper( "asset", new Helper[Any] {
        /** {@inheritDoc} */
        override def apply( value: Any, opts: Options )
            = env.loader.url( opts.fn().toString ).getOrElse("")
    });


    /** Renders the given component type with the given data */
    def apply ( template: String, data: Map[String, Any] ): String = {

        // Converts a value to a java equivalent
        def convert ( value: Any ): Any = value match {
            case list: Map[_, _] => JavaConversions.mapAsJavaMap(
                list.foldLeft( Map[Any,Any]() ) {
                    (accum, pair) => accum + (pair._1 -> convert(pair._2))
                }
            )
            case seq: Seq[_]
                => JavaConversions.asJavaIterable( seq.map( convert _ ) )
            case _ => value
        }

        engine.compile( template ).apply( Context.newBuilder(
            JavaConversions.mapAsJavaMap(
                data.foldLeft( Map[String, Any]() ) {
                    (accum, pair) => accum + (pair._1 -> convert(pair._2))
                }
            ) ).build
        )
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

