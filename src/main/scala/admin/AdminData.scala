package com.roundeights.shnappy.admin

import com.roundeights.foldout._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent._

/**
 * Admin data accessor
 */
class AdminData ( private val db: Database ) {

    // Design interface
    private val design = Await.result( db.designDir( classOf[AdminData],
        "usersByEmail" -> "/couchdb/usersByEmail"
    ), Duration(3, "second") )

    /** Returns a user by their ID */
    def getUserByEmail ( email: String ): Future[Option[User]] = {
        design.view("usersByEmail").key(email).limit(1).exec
            .map( _.headOption.map( doc => User(doc) ) )
    }

    /** Returns a user by their ID */
    def getUser ( uuid: UUID ): Future[Option[User]]
        = db.get( uuid.toString ).map( _.map( doc => User(doc) ) )

    /** Saves a User */
    def saveUser ( user: User ): Future[Written] = db.put( user )
}

