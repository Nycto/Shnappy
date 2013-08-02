package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.skene._
import com.roundeights.scalon.nObject
import com.roundeights.shnappy._
import com.roundeights.shnappy.component.Parser

/**
 * Admin handlers
 */
class AdminHandler(
    env: Env, data: AdminData, baseTemplate: Templater, parser: Parser
) extends Skene {

    /** Session token manager */
    val sessions = new Session(env.secretKey)

    /** A registry of Prereq providers */
    val prereq = Registry()
        .register[Auth]( new AuthProvider(!env.adminDevMode, sessions, data) )
        .register[Admin]( new AdminProvider )
        .register[SiteEditor]( new SiteEditorProvider )
        .register[ContentEditor]( new ContentEditorProvider )
        .register[UserParam]( new UserParamProvider(data) )
        .register[SiteParam]( new SiteParamProvider(data) )
        .register[ContentParam]( new ContentParamProvider(data) )
        .register[BodyData]( new BodyDataProvider )
        .register[Persona]( new PersonaProvider(
            audience = "%s://%s:%d".format(
                if ( env.adminDevMode ) "http" else "https",
                env.adminHost,
                if ( env.adminDevMode ) 8080 else 443
            ),
            live = !env.adminDevMode
        ))
        .register[AdminTemplate]( new AdminTemplateProvider(baseTemplate) )

    // Template builder
    val template = baseTemplate.wrap("admin/page", "content")


    /** A base handler for admin pages */
    trait BaseAdmin extends Skene {

        /** Generates a custom error message */
        def formatErr ( resp: Response, message: String ): Unit

        /** A custom error handler */
        def customErr( resp: Response ): PartialFunction[Throwable, Unit] = {
            new PartialFunction[Throwable, Unit] {
                override def apply(v1: Throwable): Unit = {}
                override def isDefinedAt(x: Throwable): Boolean = false
            }
        }

        // Centralized error handler
        error((req, resp) => customErr(resp).orElse({

            case err: BodyData.InvalidContent => {
                err.printStackTrace
                formatErr( resp.badRequest, err.getMessage )
            }

            case err: NotFound =>
                formatErr( resp.notFound, err.getMessage )

            case err: InvalidData =>
                formatErr( resp.badRequest, err.getMessage )

            case err: Unauthenticated =>
                formatErr( resp.unauthorized, "Unauthenticated request" )

            case err: Unauthorized =>
                formatErr( resp.unauthorized, "Unauthorized" )

            case err: Data.WrongType =>
                formatErr( resp.notFound, "Not Found" )

            case err: Throwable => {
                err.printStackTrace
                formatErr( resp.serverError, "Internal server error" )
            }
        }))

        // Hook in a default page handler when nothing else matches
        default( (_: Request, r: Response)
            => formatErr(r.notFound, "404 Not Found") )

        // Fail non-secure requests
        if ( !env.adminDevMode )
            notSecure(_ => throw new Insecure)
    }


    // API handlers
    request("/admin/api/**")(new BaseAdmin {

        /** {@inheritDoc} */
        override def formatErr ( resp: Response, message: String ): Unit = {
            resp.json( nObject(
                "status" -> "error",
                "message" -> message
            ).toString ).done
        }

        delegate( new AuthApiHandler(prereq, env, data, sessions) )
        delegate( new ContentApiHandler(prereq, data, parser) )
        delegate( new SiteApiHandler(prereq, data) )
        delegate( new UserApiHandler(prereq, data) )
    })


    // HTML handlers
    delegate( new BaseAdmin {

        /** {@inheritDoc} */
        override def formatErr ( resp: Response, message: String ): Unit
            = resp.html( template("admin/error", "message" -> message) ).done

        /** {@inheritDoc} */
        override def customErr(resp: Response) = {
            case err: Unauthenticated => resp.html(
                baseTemplate
                    .wrap("admin/page", "content", "enableLogin" -> true)
                    .apply("admin/login")
            ).done
        }

        delegate( new ContentHtmlHandler(template, prereq) )
    })

}


