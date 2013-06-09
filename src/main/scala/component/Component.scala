package com.roundeights.shnappy.component

import com.roundeights.scalon.{nObject, nElement, nParser}

/**
 * A component is anything that can be rendered
 */
trait Component {

    /** Renders this component */
    def render: String

    /** Serializes this component down to a JSON instance */
    def serialize: nObject
}

/** @see Parser */
object Parser {

    /** Parses an individual component */
    trait CompParser {

        /** Returns the name of this parser */
        def name: String

        /** Parses a serialized component */
        def parse( obj: nObject, nested: Parser ): Component
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
    def parse ( json: String ): Seq[Component] = parse( nParser.json(json) )

    /** Parses a json element */
    def parse ( element: nElement ): Seq[Component] = {
        element.asArray.map( _.asObject ).map(obj => {
            val compType = obj.str("type")
            parserIndex.get( compType ).getOrElse(
                throw new NoSuchElementException(
                    "Invalid component type: %s".format(compType)
                )
            ).parse( obj, this )
        })
    }

}

