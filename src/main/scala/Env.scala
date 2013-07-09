package com.roundeights.shnappy

import java.io.File
import java.util.UUID
import com.roundeights.scalon.nParser
import com.roundeights.hasher.Algo
import com.roundeights.skene.static.AssetLoader
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.jknack.handlebars.io._

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
            js = AssetLoader.fromDir( rootDir, "js" ),
            assets = AssetLoader.fromDir( rootDir, "assets" ),
            templates = new FileTemplateLoader(
                new File( rootDir, "templates" ).getAbsoluteFile
            ),
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
        templates = new URLTemplateLoader {
            override def getResource( location: String ) = {
                mainClazz.getResource(
                    "/templates/" + location.dropWhile(_ == '/')
                )
            }
        },
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
    val templates: TemplateLoader,
    val adminDevMode: Boolean = false,
    secret: Option[String] = None
) {

    /** {@inheritDoc} */
    override def toString = {
        "Env(couch: %s, db: %s, css: %s, js: %s, assets: %s)".format(
            couchDB, database, css, js, assets
        )
    }

    /** The secret key for this environment */
    val secretKey = {
        val seed = secret.getOrElse( UUID.randomUUID.toString )
        Algo.pbkdf2( seed, 1000, 512 )( seed )
    }
}

