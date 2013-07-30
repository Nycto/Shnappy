package com.roundeights.shnappy.admin

import com.roundeights.attempt._
import com.roundeights.skene.{Provider, Bundle}
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID

/** Thrown when an input field is invalid */
class InvalidData( message: String ) extends Exception( message )

/**
 * Requires that the authenticated user is an admin for a site
 */
trait SiteAdmin {

    /** The site in question */
    def siteID: UUID

    /** {@inheritDoc} */
    override def toString = "SiteAdmin(%s)".format(siteID)
}

/**
 * Builds a SiteAdmin prereq
 */
class SiteAdminProvider extends Provider[SiteAdmin] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[SiteAdmin] ): Unit = {
        val user = bundle.get[Auth].user

        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("siteID") )
            } onFailMatch {
                // FIXME: Make this a client error
                case _: Throwable =>
                    next.failure( new InvalidData("Invalid site ID") )
            }

            _ <- user.sites.contains( id ) :: OnFail {
                next.failure(new Auth.Unauthorized("User can not access site"))
            }

        } next.success(new SiteAdmin {
            override def siteID = id
        })
    }
}


