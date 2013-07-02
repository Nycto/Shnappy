package com.roundeights.shnappy.admin

import com.roundeights.foldout._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Admin data accessor
 */
class AdminData ( private val db: Database ) {

    /** Returns a user by their ID */
    def getUser ( uuid: UUID ): Future[Option[User]]
        = db.get( uuid.toString ).map( _.map( doc => User(doc) ) )

    /** Saves a User */
    def saveUser ( user: User ): Future[Written] = db.put( user )
}

