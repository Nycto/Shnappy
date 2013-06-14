package com.roundeights.shnappy

import java.io.File
import com.roundeights.scalon.nParser

/**
 * Environment information
 */
object Env {

    /** Local couch configuration */
    case class CouchDB ( host: String, port: Int, ssl: Boolean )

    /** Cloudant configuration */
    case class Cloudant ( username: String, apiKey: String, password: String )

    /** The shared environment configuration */
    lazy val env = {
        val file = new File("/home/dotcloud/environment.json")
        file.isFile match {
            case false => Env(
                couchDB = Left( CouchDB("localhost", 5984, false) ),
                database = "shnappy"
            )
            case true => {
                val json = nParser.json( file ).asObject
                Env(
                    couchDB = Right( Cloudant(
                        json.str("CLOUDANT_KEY"),
                        json.str("CLOUDANT_PASSWORD"),
                        json.str("CLOUDANT_USER")
                    ) ),
                    database = json.str("COUCHDB_DATABASE")
                )
            }
        }
    }

}

/**
 * Environment information
 */
case class Env (
    couchDB: Either[Env.CouchDB, Env.Cloudant],
    database: String
)

