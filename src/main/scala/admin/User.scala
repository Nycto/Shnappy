package com.roundeights.shnappy.admin

import com.roundeights.foldout.{Doc, Documentable}
import com.roundeights.vfunk.{Validate, Filter, TextField}
import com.roundeights.scalon._
import java.util.UUID

/** @see User */
object User {

    /** Creates a new user  */
    def apply ( name: String, email: String )
        = new User ( UUID.randomUUID, None, name, email )

    /** Creates a user from a Couch DB doc */
    def apply ( doc: Doc ) = new User (
        UUID.fromString( doc.id ),
        Some( doc.rev ),
        doc.str("name"),
        doc.str("email"),
        doc.ary_?("sites").getOrElse( nList() )
            .map( elem => UUID.fromString(elem.asString) ).toSet,
        doc.bool_?("isAdmin").getOrElse(false)
    )

    /** Validates a user's name */
    private[User] val name = TextField( "name",
        Filter.chain( Filter.printable, Filter.trim ),
        Validate.notEmpty
    )

    /** Validates a user's email address */
    val email = TextField( "email", Filter.trim, Validate.email )
}

/**
 * A user
 */
case class User (
    val id: UUID,
    private val revision: Option[String],
    rawName: String,
    rawEmail: String,
    val sites: Set[UUID] = Set(),
    val isAdmin: Boolean = false
) extends Documentable with nElement.ToJson {

    /** The filtered and validated name */
    val name = User.name.process( rawName ).require.value

    /** The filtered and validated email address */
    val email = User.email.process( rawEmail ).require.value

    /** {@inheritDoc} */
    override def toString = "User(%s, %s, %s)".format(id, name, email)

    /** Replaces the name of this user */
    def withName( newName: String )
        = User(id, revision, newName, email, sites, isAdmin)

    /** Replaces the email of this user */
    def withEmail( newEmail: String )
        = User(id, revision, name, newEmail, sites, isAdmin)

    /** Replaces the list of sites  */
    def setSites( siteList: Set[UUID] ): User
        = User(id, revision, name, email, siteList, isAdmin)

    /** Replaces this user as the admin */
    def setAdmin( admin: Boolean )
        = User(id, revision, name, email, sites, admin)

    /** {@inheritDoc} */
    override def toDoc = Doc(
        "_id" -> id.toString,
        "_rev" -> revision,
        "type" -> "user",
        "name" -> name,
        "email" -> email,
        "sites" -> sites.map( _.toString ),
        "isAdmin" -> isAdmin
    )

    /** {@inheritDoc} */
    override def toJson = nObject(
        "userID" -> id.toString,
        "name" -> name,
        "email" -> email,
        "sites" -> sites.map( _.toString ),
        "isAdmin" -> isAdmin
    )

    /** Returns whether this user can make change to a given site */
    def canChange( siteID: UUID ) = isAdmin || sites.contains(siteID)
}

