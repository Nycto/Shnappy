package com.roundeights.shnappy.admin

import com.roundeights.foldout._
import com.roundeights.shnappy._
import com.roundeights.shnappy.component.Parser
import com.roundeights.scalon._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._

/**
 * Admin data accessor
 */
class AdminData ( private val db: Database, private val parser: Parser ) {

    // Design interface
    private val design = Await.result( db.designDir( classOf[AdminData],
        "usersByEmail"  -> "/couchdb/usersByEmail",
        "sites"         -> "/couchdb/sites",
        "contentBySite" -> "/couchdb/contentBySite",
        "users"         -> "/couchdb/users",
        "usersBySiteID" -> "/couchdb/usersBySiteID"
    ), Duration(3, "second") )

    /** Returns a user by their ID */
    def getUserByEmail ( email: String ): Future[Option[User]] = {
        design.view("usersByEmail").key(email).limit(1).exec
            .map( _.headOption.map( doc => User(doc) ) )
    }

    /** Returns a user by their ID */
    def getUsersBySiteID ( siteID: UUID ): Future[Seq[User]] = {
        design.view("usersBySiteID")
            .startKey( nString(siteID.toString), nNull() )
            .endKey( nString(siteID.toString), nObject() )
            .exec
            .map( _.map( doc => User(doc) ) )
    }

    /** Returns a user by their ID */
    def getUser ( uuid: UUID ): Future[Option[User]]
        = db.get( uuid.toString ).map( _.map( doc => User(doc) ) )

    /** Returns all users */
    def getUsers: Future[Seq[User]]
        = design.view("users").exec.map( _.map( doc => User(doc) ) )

    /** Returns a list of all pages and links for a site */
    def getSites: Future[Seq[SiteInfo]]
        = design.view("sites").exec.map( _.map( doc => SiteInfo(doc) ) )

    /** Returns details for a specific site */
    def getSite( uuid: UUID ): Future[Option[SiteInfo]]
        = db.get( uuid.toString ).map( _.map( doc => SiteInfo(doc) ) )

    /** Returns a list of all pages and links for a site */
    def getPagesAndLinks( siteID: UUID ): Future[Seq[Either[Page,RawLink]]] = {
        design.view("contentBySite")
            .startKey( nString(siteID.toString), nNull() )
            .endKey( nString(siteID.toString), nObject() )
            .exec.map( _.map( doc => doc.str("type") match {
                case "page" => Left( Page(doc, parser) )
                case "link" => Right( RawLink(doc) )
            }))
    }

    /** Saves a document */
    def save ( doc: Documentable ): Future[Written] = db.put( doc )

    /** Deletes a document */
    def delete ( doc: Documentable ): Future[Written] = db.delete( doc )
}

