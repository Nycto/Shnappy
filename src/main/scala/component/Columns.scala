package com.roundeights.shnappy.component

import com.roundeights.scalon.nObject


/** @see Columns */
object Columns {

    /** Parses a columns instance */
    class Parse extends Parser.CompParser {

        /** {@inheritDoc} */
        override val name = "columns"

        /** {@inheritDoc} */
        override def parse( obj: nObject, nested: Parser ): Component = {
            new Columns( nested.parse( obj.ary("content") ) )
        }
    }
}

/**
 * Renders columns
 */
case class Columns( private val columns: Seq[Component] ) extends Component {

    /** {@inheritDoc} */
    override def render: String = {
        val total = columns.length
        columns.zipWithIndex.map {
            case (column, index) => {
                "<section class='column%dof%d'>%s</section>\n".format(
                    index + 1, total, column.render
                )
            }
        }.mkString
    }

    /** {@inheritDoc} */
    override def serialize = nObject(
        "type" -> "columns",
        "content" -> columns.map( _.serialize )
    )
}



