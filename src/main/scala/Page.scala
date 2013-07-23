package com.roundeights.shnappy

import com.roundeights.shnappy.component.{Component, Parser}
import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.scalon.nList
import com.roundeights.vfunk.{Validate, Filter, TextField}
import scala.concurrent.{Future, ExecutionContext}
import java.util.{UUID, Date}

/** @see Page */
object Page {

    /** Creates a new Page */
    def apply ( siteID: UUID, title: String, slug: String ) = new Page(
        UUID.randomUUID, None, siteID, title, slug, Nil, None, None
    )

    /** Creates a Page from a document and a parser */
    def apply ( doc: Doc, parser: Parser ) = new Page(
        UUID.fromString( doc.id ),
        Some( doc.rev ),
        UUID.fromString( doc.str("siteID") ),
        doc.str("title"),
        doc.str("slug"),
        parser.parse( doc.ary("content") ),
        doc.str_?("markedIndex").map( DateGen.parse _ ),
        doc.str_?("navSort").map( value => new SortKey(value) )
    )

    /** Filter and validation rules for a slug */
    private[Page] val slug = TextField( "slug",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.and(
            Validate.notEmpty,
            Validate.chars('a' to 'z', 'A' to 'Z', '0' to '9', Seq('_', '-'))
        )
    )

    /** Filter and validation rules for a page title */
    private[Page] val title = TextField( "title",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.notEmpty
    )
}

/**
 * An individual page
 */
case class Page (
    private val id: UUID,
    private val revision: Option[String],
    val siteID: UUID,
    rawTitle: String,
    rawSlug: String,
    val content: Seq[Component],
    private val markedIndex: Option[Date],
    private val navSort: Option[SortKey]
) extends Documentable {

    /** The filtered and validated page title */
    val title = Page.title.process( rawTitle ).require.value

    /** The filtered and validated slug */
    val slug = Page.slug.process( rawSlug ).require.value

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
        id, revision, siteID, title, slug, content,
        if ( mark ) Some( new Date ) else None,
        navSort
    )

    /** Returns a copy of this Page, but marked as the index */
    def setIndex: Page = setIndex( true )

    /** Sets the sort position of this page */
    def setNavSort( sort: Option[String] ): Page = Page(
        id, revision, siteID, title, slug, content, markedIndex,
        sort.map(key => new SortKey(key))
    )

    /** Sets the sort position of this page */
    def setNavSort( sort: String ): Page = setNavSort( Some(sort) )

    /** Sets the content of this page */
    def setContent( newContent: Seq[Component] ) = Page(
        id, revision, siteID, title, slug, newContent, markedIndex, navSort
    )

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> id.toString,
        "_rev" -> revision,
        "siteID" -> siteID.toString,
        "type" -> "page",
        "title" -> title,
        "slug" -> slug,
        "content" -> content.foldRight( nList() )( _.serialize :: _ ),
        "markedIndex" -> markedIndex.map( DateGen.format _ ),
        "navSort" -> navSort.map( _.toString )
    )
}

