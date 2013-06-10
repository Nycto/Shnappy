package com.roundeights.shnappy

import com.roundeights.shnappy.component.{Component, Parser}
import com.roundeights.foldout.{Doc, Documentable}
import scala.concurrent.{Future, ExecutionContext}
import java.util.{UUID, Date}

/** @see Page */
object Page {

    /** Creates a new Page */
    def apply ( title: String, slug: String, content: Seq[Component] )
        = new Page( UUID.randomUUID, None, title, slug, content, None, None )

    /** Creates a Page from a document and a parser */
    def apply ( doc: Doc, parser: Parser ) = new Page(
        UUID.fromString( doc.str("_id") ),
        Some( doc.str("_rev") ),
        doc.str("title"),
        doc.str("slug"),
        parser.parse( doc.ary("components") ),
        doc.str_?("markedIndex").map( DateGen.parse _ ),
        doc.str_?("navSort").map( value => new SortKey(value) )
    )
}

/**
 * An individual page
 */
case class Page (
    private val id: UUID,
    private val revision: Option[String],
    val title: String,
    val slug: String,
    private val content: Seq[Component],
    private val markedIndex: Option[Date],
    private val navSort: Option[SortKey]
) extends Nav {

    /** Renders this component */
    def render
        ( renderer: Renderer )
        ( implicit ctx: ExecutionContext )
    : Future[String]
        = Future.sequence( content.map( _.render(renderer) ) ).map(_.mkString)

    /** {@inheritDoc} */
    override def linkUrl: String = "/" + slug

    /** {@inheritDoc} */
    override def linkText: String = title

    /** {@inheritDoc} */
    override def sortKey: SortKey = navSort.get

}

