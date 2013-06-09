package com.roundeights.shnappy

import com.roundeights.foldout.CouchDB
import scala.concurrent.ExecutionContext.Implicits.global

/** @see Data */
object Data {

    /** The couch db connection */
    private lazy val db = new Data(Env.env.couchDB match {
        case Left( Env.CouchDB(host, port, ssl) )
            => CouchDB(host, port, ssl)
        case Right( Env.Cloudant(username, password) )
            => CouchDB.cloudant(username, password)
    })

    /** Returns a shared data instance */
    def apply(): Data = db
}

/**
 * Data access interface
 */
class Data ( private val db: CouchDB ) {
}

