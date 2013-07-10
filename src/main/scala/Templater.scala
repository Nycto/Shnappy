package com.roundeights.shnappy

import scala.collection.JavaConversions

import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.{Handlebars, Context, Helper, Options}
import java.io.File
import java.util.{HashMap => JavaMap}

/** @see Templater */
object Templater {

    /** Constructs a new instance */
    def apply( env: Env ) = new BaseTemplater( env )
}

/**
 * An interface for rendering templated data
 */
trait Templater {

    /** Renders the given component type with the given data */
    def apply ( template: String, data: Map[String, Any] ): String

    /** Renders the given component type with the given data */
    def apply ( template: String, data: (String, Any)* ): String
        = apply( template, Map(data:_*) )

    /** Generates a Templater that wraps other templated content */
    def wrap(
        template: String, as: String, data: Map[String, Any]
    ): Templater = {
        var outer = this
        new Templater {
            override def apply (
                innerTemplate: String, innerData: Map[String, Any]
            ): String = outer.apply(
                template,
                data + ( as -> outer.apply(innerTemplate, innerData) )
            )
        }
    }

    /** Generates a Templater that wraps other templated content */
    def wrap( template: String, as: String, data: (String, Any)* ): Templater
        = wrap(template, as, Map( data:_* ))
}

/**
 * Renders a template
 */
class BaseTemplater (
    private val env: Env,
    private val handlers: Map[String,(String) => String] = Map()
) extends Templater {

    /** Templating engine */
    private lazy val engine = {
        val engine = new Handlebars( env.templates )
        handlers.foreach( pair => {
            engine.registerHelper( pair._1, new Helper[Any] {
                override def apply( value: Any, opts: Options )
                    = pair._2( opts.fn().toString )
            });
        })
        engine
    }

    /** Registers a block handler */
    def handle( name: String, callback: (String) => String ): BaseTemplater
        = new BaseTemplater( env, handlers + (name -> callback) )

    /** Registers a block handler that expects a list of strings */
    def handleList(
        name: String, callback: (Seq[String]) => String
    ): BaseTemplater = handle( name, content => callback(
        content.split(",").map( _.trim ).filter( _ != "" )
    ))

    /** {@inheritDoc} */
    override def apply ( template: String, data: Map[String, Any] ): String = {

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

}


