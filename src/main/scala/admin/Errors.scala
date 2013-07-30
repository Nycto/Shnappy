package com.roundeights.shnappy.admin

/** Thrown when an input field is invalid */
class InvalidData( message: String ) extends Exception( message )

/** Thrown when a resource can't be found */
class NotFound( message: String ) extends Exception( message )

/** Thrown when a user isn't logged in */
class Unauthenticated( message: String ) extends Exception(message)

/** Thrown when a user fails authentication */
class Unauthorized( message: String ) extends Exception(message)

/** Thrown when a page isn't loaded via HTTPs */
class Insecure extends Exception

