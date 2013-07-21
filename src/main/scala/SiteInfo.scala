package com.roundeights.shnappy

import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.vfunk.{Validate, Filter, TextField}
import java.util.UUID

/** @see SiteInfo */
object SiteInfo {

    /** The key under which to save the site info */
    private[shnappy] val couchKey = "siteinfo"

    /** Creates a new site info instance */
    def apply ( theme: String, title: String, favicon: Option[String] )
        = new SiteInfo( None, theme, Some(title), favicon )

    /** Creates an SiteInfo from a document */
    def apply ( doc: Doc ) = new SiteInfo(
        Some( doc.rev ),
        doc.str("theme"),
        doc.str_?("title"),
        doc.str_?("favicon")
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
}

/** Represents data that applies to the whole site */
case class SiteInfo (
    private val revision: Option[String],
    rawTheme: String,
    rawTitle: Option[String],
    val favicon: Option[String]
) extends Documentable {

    /** The filtered and validated theme */
    val theme = SiteInfo.theme.process( rawTheme ).require.value

    /** The filtered and validated title */
    val title = rawTitle.map(
        value => SiteInfo.title.process( value ).require.value
    )

    /** Returns this instance as a map */
    def toMap: Map[String, String] = {
        title.foldLeft( Map("theme" -> theme) ) {
            (accum, value) => accum + ("title" -> value)
        }
    }

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> SiteInfo.couchKey,
        "_rev" -> revision,
        "theme" -> theme,
        "title" -> title,
        "favicon" -> favicon
    )
}

