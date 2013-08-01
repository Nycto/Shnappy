package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._
import scala.concurrent.Promise
import java.util.UUID



/**
 * Extracts a user being interacted with in the API
 */
trait UserEdit {

    /** The user being edited */
    def userEdit: User

    /** {@inheritDoc} */
    override def toString = "UserEdit(%s)".format( userEdit )
}

/**
 * Builds an UserEdit prereq
 */
class UserEditProvider( val data: AdminData ) extends Provider[UserEdit] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[UserEdit] ): Unit = {
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

        } next.success(new UserEdit {
            override val userEdit = user
        })
    }
}
