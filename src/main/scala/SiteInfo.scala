package com.roundeights.shnappy

import com.roundeights.foldout.{Doc, Documentable}
import java.util.UUID

/** @see SiteInfo */
object SiteInfo {

    /** The key under which to save the site info */
    val couchKey = "siteinfo"

    /** Creates a new site info instance */
    def apply ( title: String ) = new SiteInfo( None, Some(title) )

    /** Creates an SiteInfo from a document */
    def apply ( doc: Doc ) = new SiteInfo(
        Some( doc.rev ),
        doc.str_?("title")
    )
}

/** Represents data that applies to the whole site */
case class SiteInfo (
    private val revision: Option[String] = None,
    val title: Option[String] = None
) extends Documentable {

    /** Returns this instance as a map */
    def toMap: Map[String, String]
        = title.map(value => Map("title" -> value)).getOrElse( Map() )

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> SiteInfo.couchKey,
        "_rev" -> revision,
        "title" -> title
    )
}

