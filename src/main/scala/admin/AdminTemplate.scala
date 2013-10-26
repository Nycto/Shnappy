package com.roundeights.shnappy.admin

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene.{Provider, Bundle, Registry}
import com.roundeights.attempt._
import com.roundeights.shnappy.Env
import com.roundeights.tubeutil.Templater

/**
 * An admin template builder that comes prebaked with data
 */
trait AdminTemplate {

    /** Admin Template Builder */
    def template: Templater

    /** {@inheritDoc} */
    override def toString = "AdminTemplate()"
}

/**
 * Builds a Template instance with prebaked admin data
 */
class AdminTemplateProvider (
    private val env: Env,
    private val baseTemplate: Templater
) extends Provider[AdminTemplate] {

    /** {@inheritDoc} */
    override def dependencies: Set[Class[_]] = Set( classOf[Auth] )

    /** {@inheritDoc} */
    override def build( bundle: Bundle, next: Promise[AdminTemplate] ): Unit = {
        next.success( new AdminTemplate {
            override val template = baseTemplate.wrap(
                "admin/page", "content",
                "user" -> bundle.get[Auth].user,
                "enableLogin" -> true,
                "email" -> bundle.get[Auth].authEmail,
                "debug" -> env.adminDevMode,
                "isAdmin" -> bundle.get[Auth].user.isAdmin
            )
        } )
    }
}




