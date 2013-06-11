package com.roundeights.shnappy

import com.roundeights.shnappy.component.{Component, Parser}
import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.scalon.nList
import scala.concurrent.{Future, ExecutionContext}
import java.util.{UUID, Date}

/** @see Page */
object Page {

    /** Creates a new Page */
    def apply ( title: String, slug: String )
        = new Page( UUID.randomUUID, None, title, slug, Nil, None, None )

    /** Creates a Page from a document and a parser */
    def apply ( doc: Doc, parser: Parser ) = new Page(
        UUID.fromString( doc.id ),
        Some( doc.rev ),
        doc.str("title"),
        doc.str("slug"),
        parser.parse( doc.ary("content") ),
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
    val content: Seq[Component],
    private val markedIndex: Option[Date],
    private val navSort: Option[SortKey]
) extends Documentable {

    /** Renders this component */
    def render
        ( renderer: Renderer )
        ( implicit ctx: ExecutionContext )
    : Future[String]
        = Future.sequence( content.map( _.render(renderer) ) ).map(_.mkString)

    /** Returns a link to this page */
    def navLink: Option[NavLink]
        = navSort.map(sort => NavLink("/" + slug, title, sort))

    /** Returns a copy of this Page, but marked as the index */
    def setIndex( mark: Boolean ): Page = Page(
        id, revision, title, slug, content,
        if ( mark ) Some( new Date ) else None,
        navSort
    )

    /** Returns a copy of this Page, but marked as the index */
    def setIndex: Page = setIndex( true )

    /** Sets the sort position of this page */
    def setNavSort( sort: Option[String] ): Page = Page(
        id, revision, title, slug, content, markedIndex,
        sort.map(key => new SortKey(key))
    )

    /** Sets the sort position of this page */
    def setNavSort( sort: String ): Page = setNavSort( Some(sort) )

    /** Sets the content of this page */
    def setContent( newContent: Seq[Component] )
        = Page(id, revision, title, slug, newContent, markedIndex, navSort)

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> id.toString,
        "_rev" -> revision,
        "type" -> "page",
        "title" -> title,
        "slug" -> slug,
        "content" -> content.foldRight( nList() )( _.serialize :: _ ),
        "markedIndex" -> markedIndex.map( DateGen.format _ ),
        "navSort" -> navSort.map( _.toString )
    )
}

