package com.roundeights.shnappy

import java.io.File
import java.util.UUID
import com.roundeights.scalon.nParser
import com.roundeights.hasher.Algo
import com.roundeights.skene.static.AssetLoader
import scala.concurrent.ExecutionContext.Implicits.global

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

            case false => new Env(
                couchDB = Left( CouchDB("localhost", 5984, false) ),
                database = "shnappy",
                rootDir = new File( System.getProperty("user.dir") )
            )

            case true => {
                val json = nParser.json( file ).asObject

                new Env(
                    couchDB = Right( Cloudant(
                        username = json.str("CLOUDANT_USER"),
                        apiKey = json.str("CLOUDANT_KEY"),
                        password = json.str("CLOUDANT_PASSWORD")
                    ) ),
                    database = json.str("COUCHDB_DATABASE"),
                    rootDir = findRoot,
                    secret = json.str_?("SECRET_KEY")
                )
            }

        }
    }

}

/**
 * Environment information
 */
class Env (
    val couchDB: Either[Env.CouchDB, Env.Cloudant],
    val database: String,
    val rootDir: File,
    secret: Option[String] = None
) {

    /** The secret key for this environment */
    val secretKey = {
        val seed = secret.getOrElse( UUID.randomUUID.toString )
        Algo.pbkdf2( seed, 1000, 512 )( seed )
    }

    /** The resource asset loader */
    val loader = new AssetLoader( new File(rootDir, "assets"), "assets" )

}

