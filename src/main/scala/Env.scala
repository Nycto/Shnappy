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

    /** Builds a local Env object using the given root */
    def local ( rootDir: File ) = new Env(
        couchDB = Left( CouchDB("localhost", 5984, false) ),
        database = "shnappy",
        rootDir = rootDir,
        cssDir = new File(rootDir, "build/css"),
        httpsOnlyAdmin = false
    )

    /** Builds a production ready environment instance */
    def war( rootDir: File, settings: (String) => Option[String] ) = new Env(
        couchDB = Right( Cloudant(
            username = settings("CLOUDANT_USER").get,
            apiKey = settings("CLOUDANT_KEY").get,
            password = settings("CLOUDANT_PASSWORD").get
        ) ),
        database = settings("COUCHDB_DATABASE").get,
        rootDir = rootDir,
        cssDir = new File(rootDir, "css"),
        secret = settings("SECRET_KEY")
    )
}

/**
 * Environment information
 */
class Env (
    val couchDB: Either[Env.CouchDB, Env.Cloudant],
    val database: String,
    val rootDir: File,
    cssDir: File,
    val httpsOnlyAdmin: Boolean = true,
    secret: Option[String] = None
) {

    /** The secret key for this environment */
    val secretKey = {
        val seed = secret.getOrElse( UUID.randomUUID.toString )
        Algo.pbkdf2( seed, 1000, 512 )( seed )
    }

    /** A loader for accessing the CSS */
    val css = new AssetLoader( cssDir, "css" )

    /** The JavaScript asset loader */
    val js = new AssetLoader( new File(rootDir, "js"), "js" )

    /** The resource asset loader */
    val assets = new AssetLoader( new File(rootDir, "assets"), "assets" )
}

