package com.roundeights.shnappy.component

import org.pegdown.{PegDownProcessor, Extensions}
import com.roundeights.scalon.nObject


/** @see Markdown */
object Markdown {

    /** A shared markdown parser */
    lazy protected val processor = new PegDownProcessor( Extensions.ALL )

    /** Parses a markdown component */
    def parse ( obj: nObject, nested: Parser )
        = new Markdown( obj.str("content") )
}

/**
 * Renders markdown
 */
class Markdown( private val markdown: String ) extends Component {

    /** {@inheritDoc} */
    def render: String = Markdown.processor.markdownToHtml( markdown )
}


