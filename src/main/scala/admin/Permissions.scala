package com.roundeights.shnappy.admin

import com.roundeights.attempt._
import com.roundeights.skene.{Provider, Bundle}
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID

/**
 * Requires that the authenticated user has edit permissions for a site
 */
trait SiteEditor {

    /** The site in question */
    def siteID: UUID

    /** {@inheritDoc} */
    override def toString = "SiteEditor(%s)".format(siteID)
}

/**
 * Builds a SiteEditor prereq
 */
class SiteEditorProvider extends Provider[SiteEditor] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[SiteEditor] ): Unit = {
        val user = bundle.get[Auth].user

        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("siteID") )
            } onFailMatch {
                case _: Throwable =>
                    next.failure( new InvalidData("Invalid site ID") )
            }

            _ <- user.canChange( id ) :: OnFail {
                next.failure(new Unauthorized("User can not access site"))
            }

        } next.success(new SiteEditor {
            override def siteID = id
        })
    }
}

/**
 * Denotes that the authenticated user is an admin for a site
 */
trait Admin {

    /** {@inheritDoc} */
    override def toString = "Admin"
}

/**
 * Builds an Admin prereq
 */
class AdminProvider extends Provider[Admin] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[Admin] ): Unit = {
        if ( bundle.get[Auth].user.isAdmin )
            next.success(new Admin {})
        else
            next.failure(new Unauthorized("User is not an admin"))
    }
}


