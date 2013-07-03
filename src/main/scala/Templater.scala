package com.roundeights.shnappy

import scala.collection.JavaConversions

import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.{Handlebars, Context, Helper, Options}
import java.io.File
import java.util.{HashMap => JavaMap}

/**
 * Renders a template
 */
class Templater ( private val env: Env ) {

    /** Templating engine */
    private val engine = new Handlebars( env.templates )

    /** Registers a block handler */
    protected def handle( name: String, callback: (String) => String ): Unit = {
        engine.registerHelper( name, new Helper[Any] {
            override def apply( value: Any, opts: Options )
                = callback( opts.fn().toString )
        });
    }

    /** Registers a block handler that expects a list of strings */
    protected def handleList(
        name: String, callback: (Seq[String]) => String
    ): Unit = {
        handle( name, content => callback(
            content.split(",").map( _.trim ).filter( _ != "" )
        ))
    }

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

}


