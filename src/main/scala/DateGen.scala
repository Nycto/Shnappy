package com.roundeights.shnappy

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

/**
 * Generates and parses dates
 */
object DateGen {

    /** The local timezone */
    val timezone = TimeZone.getTimeZone("America/Los_Angeles")

    /** The standardized date format for parsing */
    private val input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    input.setTimeZone( timezone )

    /** The date format to use for generating dates */
    private val output = input.clone.asInstanceOf[SimpleDateFormat]
    output.setTimeZone( TimeZone.getTimeZone("GMT") )

    /** Parses a string to a date */
    def parse ( date: String ): Date = input.parse( date )

    /** Converts a date to a string */
    def format ( date: Date ): String = output.format( date )

    /** Formats the current time */
    def formatNow: String = format( new Date )

}
