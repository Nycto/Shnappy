package com.roundeights.shnappy

import com.roundeights.shnappy.component.Parser
import com.roundeights.shnappy.admin.AdminData
import com.roundeights.tubeutil.DateGen
import com.roundeights.foldout._
import com.roundeights.scalon._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._
import java.util.{UUID, Date}

/** @see Data */
object Data {

    /** Returns a shared data instance */
    def apply( env: Env, parser: Parser ): Data = new DataCache( new LiveData(
        env.database,
        parser,
        env.couchDB match {
            case Left( Env.CouchDB(host, port, ssl) )
                => CouchDB(host, port, ssl)
            case Right( conf ) => CouchDB.cloudant(
                conf.apiKey, conf.password, Some(conf.username)
            )
        }
    ))

    /** A simple builder for local debugging */
    def apply(): Data = apply( Env.local, Parser.parser )

    /** Thrown when trying to build one object from wrongly typed source data */
    class WrongType( message: String ) extends Exception(message)

    /** Executes a thunk if the type matches */
    def checktype[T] ( doc: Doc, typename: String )( thunk: => T ): T = {
        if ( !doc.contains("type" ) )
            throw new WrongType("Doc is missing the 'type' key")
        else if ( !doc.get("type").isString )
            throw new WrongType("Doc 'type' is not a string")
        else if ( doc.str("type") != typename )
            throw new WrongType("Doc is the wrong type")
        else
            thunk
    }
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
        "nav"            -> "/couchdb/nav",
        "lastUpdated"    -> "/couchdb/lastUpdated"
    ), Duration(3, "second") )

    /** {@inheritDoc} */
    override val admin = new AdminData( db, parser )

    /** {@inheritDoc} */
    def close: Unit = couch.close

    /** {@inheritDoc} */
    override def lastUpdate ( siteID: UUID ): Future[Option[Date]] = {
        design.view("lastUpdated")
            .limit(1).group.key( siteID.toString ).exec
            .map( _.headOption.map(
                row => DateGen.parse( row.doc.str("updated") )
            ) )
    }

    /** Fetches down the SiteInfo object for a host name */
    private def getSiteInfo ( host: String ): Future[Option[SiteInfo]] = {
        design.view("siteInfoByHost").limit(1).key(
            SiteInfo.host.process(host).require.value
        ).exec.map(
            _.headOption.map( row =>SiteInfo(row.doc) )
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
                .map( _.headOption.map(row => Page(row.doc, parser)) )
        }

        /** {@inheritDoc} */
        override def getIndex: Future[Option[Page]]
            = admin.getIndex( siteInfo.id )

        /** {@inheritDoc} */
        override protected def getRawNavLinks: Future[Seq[NavLink]] = {
            design.view("nav")
                .startKey( siteInfo.id.toString, nNull() )
                .endKey( siteInfo.id.toString, nObject() )
                .asc.exec
                .map( rows => NavLink.parse(rows, parser) )
        }
    }

}

/**
 * Caches the SiteData instance for a host
 */
class DataCache( private val inner: Data ) extends Data {

    /** A cache of SiteData objects keyed by host name */
    private val cache = new LazyNegativeMap[String, (Date, SiteData)]( 240000 )

    /** The date to use when a last update isn't available */
    private val defaultUpdate = new Date

    /** {@inheritDoc} */
    def admin: AdminData = inner.admin

    /** {@inheritDoc} */
    def close: Unit = inner.close

    /** {@inheritDoc} */
    override def lastUpdate ( siteID: UUID ) = inner.lastUpdate( siteID )

    /** The callback to use for filling the cache */
    private class Callback (
        private val host: String
    ) extends LazyNegativeMap.Builder[(Date, SiteData)] {

        /** Returns the last update for the given site info */
        private def lastUpdate( info: SiteInfo ): Future[Date]
            = inner.lastUpdate( info.id ).map( _.getOrElse( defaultUpdate ) )

        /** Fetches fresh data from source */
        override def build: Future[Option[(Date, SiteData)]] = {
            inner.forSite(host).flatMap( _ match {
                case None => Future.successful( None )
                case Some(data) => lastUpdate( data.siteInfo ).map(
                    updated => Some( updated -> new SiteDataCache(data) )
                )
            })
        }

        /** Checks whether an existing cache value is fresh */
        override def isFresh( data: (Date, SiteData) ): Future[Boolean]
            = lastUpdate( data._2.siteInfo ).map( !_.after( data._1 ) )
    }

    /** {@inheritDoc} */
    override def forSite( host: String ): Future[Option[SiteData]] = {
        val cleanHost = SiteInfo.host.process(host).require.value
        cache.get( cleanHost, new Callback( cleanHost ) ).map( _.map( _._2 ) )
    }
}


