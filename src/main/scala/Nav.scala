package com.roundeights.shnappy

import com.roundeights.shnappy.component.Parser
import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.vfunk.{Validate, Filter, TextField}
import com.roundeights.scalon._
import java.util.UUID

/** @see SortKey */
object SortKey {

    /** Optionally creates a new instance */
    def apply ( elem: Option[nElement] ): Option[SortKey]
        = elem.map( value => new SortKey(value.asString) )
}

/** Represents the sort position of an element in the Nav */
case class SortKey ( val keys: IndexedSeq[Int] ) {

    /** The parts of this sort key */
    def this ( keys: String ) = this(
        keys.split('.').map( _.toInt ).seq match {
            case parsed if parsed.length > 0 => parsed
            case _ => IndexedSeq(1)
        }
    )

    /** Serializes this component down to a JSON instance */
    override def toString: String = keys.mkString(".")

    /** Generates a sort key that will put another element after this one */
    def after = new SortKey( IndexedSeq( keys(0) + 1 ) )
}

/** @see NavLink */
object NavLink {

    /** Thrown when there is a problem deserializing a link */
    class InvalidType( linkType: String ) extends Exception(
        "Invalid link type: " + linkType
    )

    /** Parses a list of links */
    def parse ( docs: Seq[Doc], parser: Parser ): Seq[NavLink] = {
        docs.foldRight( List[NavLink]() ) {
            (doc, accum) => doc.str("type") match {
                case "page" => Page(doc, parser).navLink match {
                    case Some(link) => link +: accum
                    case None => accum
                }
                case "link" => RawLink(doc).link +: accum
                case other => throw new InvalidType(other)
            }
        }
    }

    /** Creates a new NavLink */
    def apply( url: String, text: String, sort: SortKey )
        = new NavLink(url, text, sort)

    /** Filter and validation rules for a link title */
    private[NavLink] val text = TextField( "text",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.notEmpty
    )

    /** Filter and validation rules for a link URL */
    private[NavLink] val url = TextField( "url",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.and( Validate.notEmpty, Validate.noWhitespace )
    )

}

/** A link in the navigation */
class NavLink (
    rawUrl: String, rawText: String, val sort: SortKey
) extends Equals {

    /** The filtered and validated link URL */
    val url = NavLink.url.process( rawUrl ).require.value

    /** The filtered and validated link text */
    val text = NavLink.text.process( rawText ).require.value

    /** Returns this nav instance is a map of data */
    def toMap: Map[String, String] = Map("url" -> url, "text" -> text)

    /** Sets the URL of this link */
    def withURL( newURL: String ) = NavLink( newURL, text, sort )

    /** {@inheritDoc} */
    override def toString = "NavLink(%s, %s, %s)".format(url, text, sort)

    /** {@inheritDoc} */
    override def equals ( that: Any ): Boolean = that match {
        case thatLink: NavLink if thatLink.canEqual(this) => {
            url == thatLink.url &&
            text == thatLink.text &&
            sort == thatLink.sort
        }
        case _ => false
    }

    /** {@inheritDoc} */
    override def canEqual ( that: Any ) = that.isInstanceOf[NavLink]

    /** {@inheritDoc} */
    override def hashCode
        = 41 * ( 41 * ( 41 + url.hashCode ) + text.hashCode ) + sort.hashCode
}

/** @see RawLink */
object RawLink {

    /** Creates a new link */
    def apply ( siteID: UUID, url: String, text: String, sort: String ) = {
        new RawLink(
            UUID.randomUUID, None, siteID,
            NavLink( url, text, new SortKey(sort) )
        )
    }

    /** Creates an RawLink from a document */
    def apply ( doc: Doc ) = Data.checktype(doc, "link") {
        new RawLink(
            UUID.fromString( doc.id ),
            Some( doc.rev ),
            UUID.fromString( doc.str("siteID") ),
            NavLink(
                doc.str("url"), doc.str("text"),
                new SortKey( doc.get("navSort").asString )
            )
        )
    }
}

/** Represents a link without an associated page */
case class RawLink (
    private val id: UUID,
    private val revision: Option[String],
    val siteID: UUID,
    val link: NavLink
) extends Documentable with nElement.ToJson {

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> id.toString,
        "_rev" -> revision,
        "type" -> "link",
        "siteID" -> siteID.toString,
        "text" -> link.text,
        "url" -> link.url,
        "navSort" -> link.sort.toString,
        "updated" -> DateGen.formatNow
    )

    /** {@inheritDoc} */
    override def toJson = nObject(
        "contentID" -> id.toString,
        "type" -> "link",
        "siteID" -> siteID.toString,
        "navSort" -> link.sort.toString,
        "text" -> link.text,
        "url" -> link.url
    )
}

