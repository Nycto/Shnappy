package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._
import com.roundeights.shnappy._
import scala.concurrent.Promise
import java.util.UUID


/**
 * Helper methods for extracting request params
 */
object Params {

    /** Parses a UUID */
    def uuid(
        fail: Promise[_], name: String, content: => String
    ): Option[UUID] = {
        try {
            Some( UUID.fromString( content ) )
        }
        catch {
            case _: IllegalArgumentException => {
                fail.failure( new InvalidData("Invalid " + name) )
                None
            }
            case err: Throwable => {
                fail.failure(err)
                None
            }
        }
    }

}


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

            id <- Params.uuid(next, "user ID", bundle.request.params("userID"))

            user <- TryTo.lift {
                data.getUser(id) :: OnFail.alsoFail(next)
            } onFail {
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

            id <- Params.uuid(next, "site ID", bundle.request.params("siteID"))

            site <- TryTo.lift {
                data.getSite(id) :: OnFail.alsoFail(next)
            } onFail {
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

            id <- Params.uuid(
                next, "content ID", bundle.request.params("contentID")
            )

            content <- TryTo.lift {
                data.getContent(id) :: OnFail.alsoFail(next)
            } onFail {
                next.failure( new NotFound("Content does not exist") )
            }

        } next.success(new ContentParam {
            override val contentParam = content
        })
    }
}



