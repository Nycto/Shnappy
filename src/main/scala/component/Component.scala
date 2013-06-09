package com.roundeights.shnappy.component

import com.roundeights.scalon.{nObject, nElement, nParser}

/**
 * A component is anything that can be rendered
 */
trait Component {

    /** Renders this component */
    def render: String
}

/** @see Parser */
object Parser {

    /** The signature for parsing functions */
    type ParseMethod = (nObject, Parser) => Component

    /** The shared parser */
    val parser = new Parser( Map(
        "markdown" -> Markdown.parse
    ) )
}

/**
 * Parses a list of Components serialized as a JSON array
 */
class Parser ( private val parsers: Map[String, Parser.ParseMethod] ) {

    /** Parses a json string */
    def parse ( json: String ): Seq[Component] = parse( nParser.json(json) )

    /** Parses a json element */
    def parse ( element: nElement ): Seq[Component] = Nil

}

