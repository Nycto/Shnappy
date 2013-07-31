package com.roundeights.shnappy

import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.scalon.{nObject, nElement}
import com.roundeights.vfunk.{Validate, Filter, TextField, Err}
import java.util.UUID

/** @see SiteInfo */
object SiteInfo {

    /** Creates a new site info instance */
    def apply (
        theme: String, title: String,
        favicon: Option[String], hosts: Set[String]
    ) = new SiteInfo(
        UUID.randomUUID, None, theme, Some(title), favicon, hosts
    )

    /** Creates a new site info instance */
    def apply (
        theme: String, title: String, favicon: Option[String], host: String
    ): SiteInfo = apply( theme, title, favicon, Set(host) )

    /** Creates an SiteInfo from a document */
    def apply ( doc: Doc ) = new SiteInfo(
        UUID.fromString( doc.id ),
        Some( doc.rev ),
        doc.str("theme"),
        doc.str_?("title"),
        doc.str_?("favicon"),
        doc.ary_?("hosts").map( _.map( _.asString ).toSet ).getOrElse( Set() )
    )

    /** Filter and validation rules for the theme */
    private[SiteInfo] val theme = TextField( "theme",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.notEmpty
    )

    /** Filter and validation rules for the title */
    private[SiteInfo] val title = TextField( "title",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.notEmpty
    )

    /** Filter and validation rules for hosts */
    val host = TextField( "host",
        Filter.chain(
            Filter.trim, Filter.lower,
            Filter.characters( Set('.', '-') ++ ('a' to 'z') ++ ('0' to '9') ),
            Filter.callback(host =>
                if ( host.startsWith("www.") ) host.drop(4) else host
            )
        ),
        Validate.and(
            Validate.notEmpty,
            Validate.invoke( host =>
                Some( Err("HOST", "Host name must not start with a period") )
                    .filter( _ => host.startsWith(".") )
            ),
            Validate.invoke( host =>
                Some( Err("HOST", "Host name must not end with a period") )
                    .filter( _ => host.endsWith(".") )
            )
        )
    )
}

/** Represents data that applies to the whole site */
case class SiteInfo (
    val id: UUID,
    private val revision: Option[String],
    rawTheme: String,
    rawTitle: Option[String],
    val favicon: Option[String],
    rawHosts: Set[String]
) extends Documentable with nElement.ToJson {

    /** The filtered and validated theme */
    val theme: String = SiteInfo.theme.process( rawTheme ).require.value

    /** The filtered and validated title */
    val title: Option[String] = rawTitle.map(
        value => SiteInfo.title.process( value ).require.value
    )

    /** The filtered and validated title */
    val hosts: Set[String] = rawHosts.map(
        value => SiteInfo.host.process( value ).require.value
    )

    /** Changes the Theme for this site */
    def withTheme ( newTheme: String )
        = SiteInfo(id, revision, newTheme, title, favicon, hosts)

    /** Changes the title for this site */
    def withTitle ( newTitle: Option[String] )
        = SiteInfo(id, revision, theme, newTitle, favicon, hosts)

    /** Changes the favicon for this site */
    def withFavicon ( newFavicon: Option[String] )
        = SiteInfo(id, revision, theme, title, newFavicon, hosts)

    /** Changes the favicon for this site */
    def withHosts ( newHosts: Set[String] )
        = SiteInfo(id, revision, theme, title, favicon, newHosts)

    /** Returns this instance as a map */
    def toMap: Map[String, String] = {
        title.foldLeft( Map("theme" -> theme) ) {
            (accum, value) => accum + ("title" -> value)
        }
    }

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> id.toString,
        "_rev" -> revision,
        "type" -> "siteinfo",
        "theme" -> theme,
        "title" -> title,
        "favicon" -> favicon,
        "hosts" -> hosts,
        "updated" -> DateGen.formatNow
    )

    /** {@inheritDoc} */
    override def toJson = nObject(
        "siteID" -> id.toString, "theme" -> theme, "title" -> title,
        "favicon" -> favicon, "hosts" -> hosts
    )
}

