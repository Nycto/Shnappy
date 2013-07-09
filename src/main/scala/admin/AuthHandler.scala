package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject


/** Authentication handlers */
class AuthApiHandler( req: Registry ) extends Skene {

    put("/admin/api/login")(req.use[BodyData].in((prereqs, resp, recover) => {
        resp.json( nObject("status" -> "ok").toString ).done
    }))

    put("/admin/api/logout")(req.use[Auth].in((prereqs, resp, recover) => {
        resp.json( nObject("status" -> "ok").toString ).done
    }))

}


