package com.roundeights.shnappy.component

import scala.concurrent.Future
import com.roundeights.shnappy.Renderer
import org.pegdown.{PegDownProcessor, Extensions}
import com.roundeights.scalon.nObject


/** @see Markdown */
object Markdown {

    /** A shared markdown parser */
    lazy protected val processor = new PegDownProcessor( Extensions.ALL )

    /** Parses a markdown instance */
    class Parse extends Parser.CompParser {

        /** {@inheritDoc} */
        override val name = "markdown"

        /** {@inheritDoc} */
        override def parse( obj: nObject, nested: Parser.Nested ): Component
            = new Markdown( obj.str("content") )
    }
}

/**
 * Renders markdown
 */
case class Markdown( private val markdown: String ) extends Component {

    /** {@inheritDoc} */
    override def render( renderer: Renderer ): Future[String] = {
        Future.successful( renderer(
            "markdown",
            "content" ->Markdown.processor.markdownToHtml( markdown )
        ))
    }

    /** {@inheritDoc} */
    override def serialize
        = nObject( "type" -> "markdown", "content" -> markdown )
}


