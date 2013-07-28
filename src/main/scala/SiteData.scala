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


/**
 * Caches the results of a wrapped SiteData instance
 */
class SiteDataCache
    ( private val inner: SiteData )
    ( implicit val ctx: ExecutionContext )
extends SiteData {

    /** A cache of page data by slug */
    private val pages = new LazyMap[String, Option[Page]]

    /** The index */
    private val index = new LazyRef[Option[Page]]

    /** Nav links */
    private val navLinks = new LazyRef[Seq[NavLink]]

    /** {@inheritDoc} */
    override def siteInfo: SiteInfo = inner.siteInfo

    /** {@inheritDoc} */
    override def getPage ( slug: String )
        = pages.get( slug, () => inner.getPage(slug) )

    /** {@inheritDoc} */
    override def getIndex = index( inner.getIndex )

    /** {@inheritDoc} */
    override def getNavLinks = navLinks( inner.getNavLinks )
}

