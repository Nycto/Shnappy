package com.roundeights.shnappy

import scala.concurrent._

/**
 * Represents a data interface for a specific site
 */
trait SiteData {

    /** Returns the SiteInfo object */
    def siteInfo: SiteInfo

    /** Returns a page */
    def getPage ( slug: String ): Future[Option[Page]]

    /** Returns the index */
    def getIndex: Future[Option[Page]]

    /** Returns the list of navigation links */
    def getNavLinks: Future[Seq[NavLink]]
}

