package com.roundeights.shnappy.component

import scala.concurrent.Future
import com.roundeights.shnappy.Renderer
import com.roundeights.scalon.nObject


/** @see HTML */
object HTML {

    /** Parses a HTML instance */
    class Parse extends Parser.CompParser {

        /** {@inheritDoc} */
        override val name = "html"

        /** {@inheritDoc} */
        override def parse( obj: nObject, nested: Parser.Nested ): Component
            = new HTML( obj.str("content") )
    }
}

/**
 * Renders html
 */
case class HTML( private val html: String ) extends Component {

    /** {@inheritDoc} */
    override def render( renderer: Renderer ): Future[String]
        = Future.successful( renderer( "html", "content" -> html) )

    /** {@inheritDoc} */
    override def serialize = nObject( "type" -> "html", "content" -> html )
}


