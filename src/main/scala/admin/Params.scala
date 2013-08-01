package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._
import com.roundeights.shnappy._
import scala.concurrent.Promise
import java.util.UUID



/**
 * Extracts a user being interacted with in the API
 */
trait UserParam {

    /** The user being edited */
    def userParam: User

    /** {@inheritDoc} */
    override def toString = "UserParam(%s)".format( userParam )
}

/**
 * Builds an UserParam prereq
 */
class UserParamProvider( val data: AdminData ) extends Provider[UserParam] {

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[UserParam] ): Unit = {
        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("userID") )
            } onFailMatch {
                case _: Throwable =>
                    next.failure( new InvalidData("Invalid user ID") )
            }

            userOpt <- data.getUser(id) :: OnFail.alsoFail(next)

            user <- userOpt :: OnFail {
                next.failure( new NotFound("User does not exist") )
            }

        } next.success(new UserParam {
            override val userParam = user
        })
    }
}


/**
 * Extracts a site being interacted with in the API
 */
trait SiteParam {

    /** The site being edited */
    def siteParam: SiteInfo

    /** {@inheritDoc} */
    override def toString = "SiteParam(%s)".format( siteParam )
}

/**
 * Builds an SiteParam prereq
 */
class SiteParamProvider( val data: AdminData ) extends Provider[SiteParam] {

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[SiteParam] ): Unit = {
        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("siteID") )
            } onFailMatch {
                case _: Throwable => next.failure(
                    new InvalidData("Invalid site ID")
                )
            }

            opt <- data.getSite(id) :: OnFail.alsoFail(next)

            site <- opt :: OnFail {
                next.failure( new NotFound("Site does not exist") )
            }

        } next.success(new SiteParam {
            override val siteParam = site
        })
    }
}

/**
 * Extracts a piece of content being interacted with in the API
 */
trait ContentParam {

    /** The content being engaged */
    def contentParam: Either[Page, RawLink]

    /** {@inheritDoc} */
    override def toString = "Content(%s)".format( contentParam )
}

/**
 * Builds an ContentParam prereq
 */
class ContentParamProvider(
    val data: AdminData
) extends Provider[ContentParam] {

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[ContentParam] ): Unit = {
        for {

            id <- TryTo.except {
                UUID.fromString( bundle.request.params("contentID") )
            } onFailMatch {
                case _: Throwable => next.failure(
                    new InvalidData("Invalid Content ID")
                )
            }

            opt <- data.getContent(id) :: OnFail.alsoFail(next)

            content <- opt :: OnFail {
                next.failure( new NotFound("Content does not exist") )
            }

        } next.success(new ContentParam {
            override val contentParam = content
        })
    }
}



