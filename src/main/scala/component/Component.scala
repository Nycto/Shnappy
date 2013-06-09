package com.roundeights.shnappy.component

import scala.concurrent.{Future, ExecutionContext}
import com.roundeights.shnappy.Renderer
import com.roundeights.scalon.{nObject, nElement, nParser}

/**
 * A component is anything that can be rendered
 */
trait Component {

    /** Renders this component */
    def render( renderer: Renderer ): Future[String]

    /** Serializes this component down to a JSON instance */
    def serialize: nObject
}

/**
 * A page level document
 */
case class Content (
    private val comps: Seq[Component]
) {

    /** Renders this component */
    def render
        ( renderer: Renderer )
        ( implicit ctx: ExecutionContext )
    : Future[String]
        = Future.sequence( comps.map( _.render(renderer) ) ).map( _.mkString )

    /** Serializes this component down to a JSON instance */
    def serialize = nObject(
        "components" -> comps.map( _.serialize )
    )
}

/** @see Parser */
object Parser {

    /** The signature for a nested parser */
    type Nested = (nElement) => Component

    /** Parses an individual component */
    trait CompParser {

        /** Returns the name of this parser */
        def name: String

        /** Parses a serialized component */
        def parse( obj: nObject, nested: Nested ): Component
    }


    /** The shared parser */
    val parser = new Parser(
         new Markdown.Parse,
         new Columns.Parse
    )
}

/**
 * Parses a list of Components serialized as a JSON array
 */
class Parser ( parsers: Parser.CompParser* ) {

    /** A map of parsers indexed by their name */
    private val parserIndex = {
        parsers.foldLeft( Map[String, Parser.CompParser]() ) {
            (accum, parser) => accum + (parser.name -> parser)
        }
    }

    /** Parses a json string */
    def parse ( json: String ): Content = parse( nParser.json(json) )

    /** Parses a json element */
    def parse ( element: nElement ): Content = {

        // Parses an individual component
        def parseComp( elem: nElement ): Component = {
            val obj = elem.asObject
            val compType = obj.str("type")
            parserIndex.get( compType ).getOrElse(
                throw new NoSuchElementException(
                    "Invalid component type: %s".format(compType)
                )
            ).parse( obj, parseComp )
        }

        Content( element.asObject.ary("components").map( parseComp _ ) )
    }

}

