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

    /** Attempts to find the root rsync directory for this deployment */
    private def findRoot: File = {
        val matches = new File("/home/dotcloud")
            .listFiles
            .filter( _.isDirectory )
            .filter( file => {
                """rsync-[0-9]+""".r.findFirstIn( file.getName ).nonEmpty
            })

        if ( matches.length > 1 )
            throw new RuntimeException("Multiple rsync-* directories found")
        else if ( matches.length == 0 )
            throw new RuntimeException("No rsync-* directory found")
        else
            matches(0)
    }

    /** The shared environment configuration */
    lazy val env = {
        val file = new File("/home/dotcloud/environment.json")
        file.isFile match {

            case false => Env(
                couchDB = Left( CouchDB("localhost", 5984, false) ),
                database = "shnappy",
                rootDir = new File( System.getProperty("user.dir") )
            )

            case true => {
                val json = nParser.json( file ).asObject

                Env(
                    couchDB = Right( Cloudant(
                        username = json.str("CLOUDANT_USER"),
                        apiKey = json.str("CLOUDANT_KEY"),
                        password = json.str("CLOUDANT_PASSWORD")
                    ) ),
                    database = json.str("COUCHDB_DATABASE"),
                    rootDir = findRoot
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
    database: String,
    rootDir: File
)

