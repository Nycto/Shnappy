package com.roundeights.shnappy

import com.roundeights.shnappy.component.Parser
import com.roundeights.shnappy.admin.AdminData
import com.roundeights.foldout._
import com.roundeights.scalon._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._
import java.util.{UUID, Date}

/** @see Data */
object Data {

    /** Returns a shared data instance */
    def apply( env: Env, parser: Parser ): Data = new LiveData(
        env.database,
        parser,
        env.couchDB match {
            case Left( Env.CouchDB(host, port, ssl) )
                => CouchDB(host, port, ssl)
            case Right( conf ) => CouchDB.cloudant(
                conf.apiKey, conf.password, Some(conf.username)
            )
        }
    )
}

/**
 * Data access interface
 */
trait Data {

    /** Returns the interface for requesting admin data */
    def admin: AdminData

    /** Shutsdown this connection */
    def close: Unit

    /** Returns when a site was last updated */
    def lastUpdate ( siteID: UUID ): Future[Option[Date]]

    /** Returns when the SiteData instance given a host name */
    def forSite( host: String ): Future[Option[SiteData]]
}

/**
 * Data access interface
 */
class LiveData (
    database: String, private val parser: Parser, couch: CouchDB
) extends Data {

    // Make sure the database exists
    private val db = couch.db( database )
    db.createNow

    // Design interface
    private val design = Await.result( db.designDir( classOf[Data],
        "siteInfoByHost" -> "/couchdb/siteInfoByHost",
        "pagesBySlug"    -> "/couchdb/pagesBySlug",
        "pagesByIndex"   -> "/couchdb/pagesByIndex",
        "nav"            -> "/couchdb/nav",
        "lastUpdated"    -> "/couchdb/lastUpdated"
    ), Duration(3, "second") )

    /** {@inheritDoc} */
    override val admin = new AdminData( db )

    /** {@inheritDoc} */
    def close: Unit = couch.close

    /** {@inheritDoc} */
    override def lastUpdate ( siteID: UUID ): Future[Option[Date]] = {
        design.view("lastUpdated")
            .limit(1).group.key( siteID.toString ).exec
            .map( _.headOption.map(
                doc => DateGen.parse( doc.str("updated") )
            ) )
    }

    /** Fetches down the SiteInfo object for a host name */
    private def getSiteInfo ( host: String ): Future[Option[SiteInfo]] = {
        design.view("siteInfoByHost").limit(1).key(
            SiteInfo.host.process(host).require.value
        ).exec.map(
            _.headOption.map( doc =>SiteInfo(doc) )
        )
    }

    /** {@inheritDoc} */
    override def forSite( host: String ): Future[Option[SiteData]]
        = getSiteInfo( host ).map( _.map( info => new LiveSiteData(info) ) )

    /** Provides request specific data access */
    class LiveSiteData ( override val siteInfo: SiteInfo ) extends SiteData {

        /** {@inheritDoc} */
        override def getPage ( slug: String ): Future[Option[Page]] = {
            design.view("pagesBySlug")
                .key( siteInfo.id.toString, slug )
                .limit(1).exec
                .map( _.headOption.map(doc => Page(doc, parser)) )
        }

        /** {@inheritDoc} */
        override lazy val getIndex: Future[Option[Page]] = {
            design.view("pagesByIndex")
                .startKey( siteInfo.id.toString, nObject() )
                .endKey( siteInfo.id.toString, nNull() )
                .limit(1).desc.exec
                .map( _.headOption.map(doc => Page(doc, parser)) )
        }

        /** {@inheritDoc} */
        override lazy val getNavLinks: Future[Seq[NavLink]] = {
            val index: Future[Option[NavLink]]
                = getIndex.map( _.flatMap( _.navLink ) )

            design.view("nav")
                .startKey( siteInfo.id.toString, nNull() )
                .endKey( siteInfo.id.toString, nObject() )
                .asc.exec
                .map( rows => NavLink.parse(rows, parser) )
                .flatMap( links => index.map({
                    case None => links
                    case Some(indexLink) => links.map( link =>
                        if (link == indexLink) link.withURL("/") else link
                    )
                }))
        }
    }

}


