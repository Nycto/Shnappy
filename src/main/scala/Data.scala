package com.roundeights.shnappy

import com.roundeights.shnappy.component.Parser
import com.roundeights.shnappy.admin.AdminData
import com.roundeights.foldout._
import com.roundeights.skene.{Request => SkeneRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._

/** @see Data */
object Data {

    /** Returns a shared data instance */
    def apply( env: Env, parser: Parser ): Data = new Data(
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
class Data ( database: String, private val parser: Parser, couch: CouchDB ) {

    // Make sure the database exists
    val db = couch.db( database )
    db.createNow

    /** Admin data access */
    val admin = new AdminData( db )

    // Design interface
    private val design = Await.result( db.designDir( classOf[Data],
        "pagesBySlug" -> "/couchdb/pagesBySlug",
        "pagesByIndex" -> "/couchdb/pagesByIndex",
        "nav" -> "/couchdb/nav"
    ), Duration(3, "second") )

    /** Returns a new Data instance customized for the given request */
    def forSite : Future[Option[Request]] = {
        db.get( SiteInfo.couchKey ).map( _.map(
            doc => new Request( SiteInfo(doc) )
        ) )
    }

    /** Provides request specific data access */
    class Request ( val siteInfo: SiteInfo ) {

        /** Returns a page */
        def getPage ( slug: String ): Future[Option[Page]] = {
            design.view("pagesBySlug").key(slug).limit(1).exec
                .map( _.headOption.map(doc => Page(doc, parser)) )
        }

        /** Returns the index */
        lazy val getIndex: Future[Option[Page]] = {
            design.view("pagesByIndex").limit(1).desc.exec
                .map( _.headOption.map(doc => Page(doc, parser)) )
        }

        /** Returns the list of navigation links */
        lazy val getNavLinks: Future[Seq[NavLink]] = {
            val index: Future[Option[NavLink]]
                = getIndex.map( _.flatMap( _.navLink ) )

            design.view("nav").asc.exec
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

