package com.roundeights.shnappy

import java.util.{Date, UUID}
import com.roundeights.hasher.{Algo, Hash}

class Session ( secret: String ) {

    /** Returns the HMAC seed needed for the given time offset */
    private def timeSeed ( offset: Int ): String = {
        assert( offset >= 0 )
        val time = new Date().getTime / 1000
        val seed = time - (time % 60) - (60 * offset)
        Algo.hmac( secret ).sha512( seed.toString ).hex
    }

    /** HMACs the given value at the given time offset */
    def hmac ( value: String, offset: Int ): Hash
        = Algo.hmac( timeSeed(offset) + secret ).sha512( value )

    /** Generates a session token for the given user */
    def token ( user: User ): String
        = user.id + hmac( user.id.toString, 0 ).hex

    /** Checks a token to see if it is valid */
    def checkToken ( token: String ): Option[UUID] = {
        val uuid = token.take(36)
        val checksum = Hash( token.drop(36).take(128) )

        def checkOffset( offset: Int ): Option[UUID] = {
            if ( offset > 15 )
                None
            else if ( checksum == hmac(uuid, offset) )
                Some( UUID.fromString(uuid) )
            else
                checkOffset( offset + 1 )
        }

        checkOffset(0)
    }

}

