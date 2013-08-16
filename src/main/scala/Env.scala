package com.roundeights.shnappy

import java.io.File
import java.util.UUID
import com.roundeights.scalon.nParser
import com.roundeights.hasher.Algo
import com.roundeights.tubeutil.static.AssetLoader
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Environment information
 */
object Env {

    /** Local couch configuration */
    case class CouchDB ( host: String, port: Int, ssl: Boolean )

    /** Cloudant configuration */
    case class Cloudant (
        username: String, apiKey: String, password: String
    ) {
        /** {@inheritDoc} */
        override def toString = "Cloudant(%s, %s)".format(username, apiKey)
    }

    /** Builds a local Env object using the given root */
    def local = {
        val rootDir = new File( System.getProperty("user.dir") )
        new Env(
            couchDB = Left( CouchDB("localhost", 5984, false) ),
            database = "shnappy",
            css = AssetLoader.fromDir(
                new File(rootDir, "target/resources"), "css"
            ),
            js = AssetLoader.fromDir(
                new File(rootDir, "target/resources"), "js"
            ),
            assets = AssetLoader.fromDir( rootDir, "assets" ),
            templates = Templater.inDir( new File(rootDir, "templates") ),
            adminHost = "127.0.0.1",
            adminDevMode = true
        )
    }

    /** Extracts a setting */
    private def require( settings: (String) => Option[String], key: String ) = {
        settings(key).getOrElse(
            throw new java.util.NoSuchElementException(
                "Required environment variable is not set: %s".format(key)
            )
        )
    }

    /** Builds a production ready environment instance */
    def prod(
        settings: (String) => Option[String],
        mainClazz: Class[_]
    ) = new Env(
        couchDB = Right( Cloudant(
            username = require(settings, "CLOUDANT_USER"),
            apiKey = require(settings, "CLOUDANT_KEY"),
            password = require(settings, "CLOUDANT_PASSWORD")
        ) ),
        database = require(settings, "COUCHDB_DATABASE"),
        css = AssetLoader.fromJar( mainClazz, "css" ),
        js = AssetLoader.fromJar( mainClazz, "js" ),
        assets = AssetLoader.fromJar( mainClazz, "assets" ),
        templates = Templater.inJar( mainClazz, "templates" ),
        adminHost = require(settings, "ADMIN_HOST"),
        secret = settings("SECRET_KEY")
    )
}

/**
 * Environment information
 */
class Env (
    val couchDB: Either[Env.CouchDB, Env.Cloudant],
    val database: String,
    val css: AssetLoader,
    val js: AssetLoader,
    val assets: AssetLoader,
    val templates: Templater.Finder,
    val adminHost: String,
    val adminDevMode: Boolean = false,
    secret: Option[String] = None
) {

    /** {@inheritDoc} */
    override def toString = {
        val props = Map[String, Any](
            "couch" -> couchDB, "db" -> database,
            "css" -> css, "js" -> js, "assets" -> assets,
            "templates" -> templates,
            "adminHost" -> adminHost, "adminDevMode" -> adminDevMode
        )
        "Env(%s)".format(
            props.map(pair => "%s: %s".format(pair._1, pair._2)).mkString(", ")
        )
    }

    /** The secret key for this environment */
    val secretKey = {
        val seed = secret.getOrElse( UUID.randomUUID.toString )
        Algo.pbkdf2( seed, 1000, 512 )( seed )
    }
}

