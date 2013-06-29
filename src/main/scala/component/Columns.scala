package com.roundeights.shnappy.component

import scala.concurrent.Future
import com.roundeights.scalon.nObject
import com.roundeights.shnappy.Renderer


/** @see Columns */
object Columns {

    /** Parses a columns instance */
    class Parse extends Parser.CompParser {

        /** {@inheritDoc} */
        override val name = "columns"

        /** {@inheritDoc} */
        override def parse( obj: nObject, nested: Parser.Nested ): Component
            = new Columns( obj.ary("content").map( nested ) )
    }
}

/**
 * Renders columns
 */
case class Columns(
    private val columns: Seq[Component] = Seq()
) extends Component {

    /** Prepends a component */
    def :: ( component: Component ): Columns
        = Columns( component +: columns )

    /** {@inheritDoc} */
    override def render( renderer: Renderer ): Future[String] = {
        Future.successful( renderer(
            "columns",
            "total" -> columns.length,
            "columns" -> columns.zipWithIndex.foldLeft(Map[String,Any]()) {
                case (accum, (component, index)) => {
                    accum +
                        ("index" -> index) +
                        ("content" -> component.render(renderer))
                }
            }
        ))
    }

    /** {@inheritDoc} */
    override def serialize = nObject(
        "type" -> "columns",
        "content" -> columns.map( _.serialize )
    )
}



