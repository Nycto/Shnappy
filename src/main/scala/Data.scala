package com.roundeights.shnappy

import com.roundeights.shnappy.component.Parser
import com.roundeights.foldout._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._

/** @see Data */
object Data {

    /** The private couch DB instance */
    private val couchDB = Env.env.couchDB match {
        case Left( Env.CouchDB(host, port, ssl) )
            => CouchDB(host, port, ssl)
        case Right( Env.Cloudant(username, password) )
            => CouchDB.cloudant(username, password)
    }

    /** The couch db connection */
    private lazy val db = new Data(
        Env.env.database,
        Parser.parser,
        couchDB
    )

    /** Returns a shared data instance */
    def apply(): Data = db
}

/**
 * Data access interface
 */
class Data ( database: String, private val parser: Parser, couch: CouchDB ) {

    // Make sure the database exists
    private val db = couch.db( database )
    db.createNow

    // Design interface
    private val design = Await.result( db.designDir(
        "pagesBySlug" -> "/couchdb/pagesBySlug",
        "pagesByIndex" -> "/couchdb/pagesByIndex",
        "nav" -> "/couchdb/nav"
    ), Duration(3, "second") )

    /** Returns a page */
    def getPage ( slug: String ): Future[Option[Page]] = {
        design.view("pagesBySlug").key(slug).limit(1).exec
            .map( _.headOption.map(doc => Page(doc, parser)) )
    }

    /** Saves a page */
    def savePage ( page: Page ): Future[Written] = db.put( page )

    /** Returns the index */
    def getIndex: Future[Option[Page]] = {
        design.view("pagesByIndex").limit(1).desc.exec
            .map( _.headOption.map(doc => Page(doc, parser)) )
    }

    /** Returns the index */
    def getNavLinks: Future[Seq[NavLink]] = {
        design.view("nav").asc.exec.map(_.foldRight( List[NavLink]() ){
            (doc, accum) => Page(doc, parser).navLink match {
                case Some(link) => link :: accum
                case None => accum
            }
        })
    }

}

