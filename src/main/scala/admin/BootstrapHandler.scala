package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.roundeights.skene._
import com.roundeights.tubeutil.BodyData
import com.roundeights.shnappy._
import com.roundeights.shnappy.component.Markdown
import com.roundeights.scalon._
import com.roundeights.attempt._
import com.roundeights.vfunk.InvalidValueException

/**
 * An endpoint for initializing data in a site
 */
class BootstrapApiHandler(
    env: Env, req: Registry, data: AdminData
) extends Skene {

    // Fills an empty instance with data
    post("/admin/api/bootstrap") {
        req.use[BodyData].in((prereqs, resp, recover) => {
            val json = prereqs.json

            for {

                // Validate that the bootstrap key matches
                _ <- TryTo.except {
                    TryTo {
                        Some(json.str("bootstrapKey")) == env.bootstrapKey ||
                        env.adminDevMode
                    } onFail {
                        recover.orRethrow( new Unauthorized(
                            "Bootstrap key mismatch"
                        ) )
                    }
                } onFailMatch( recover.matcher {
                    case err: nException => new InvalidData( err )
                } )

                // Create the user object
                user <- TryTo.except {
                    User( json.str("name"), json.str("email") )
                } onFailMatch( recover.matcher {
                    case err@( _:nException | _:InvalidValueException )
                        => new InvalidData( err )
                } )

                // Create the SiteInfo object
                site <- TryTo.except {
                    SiteInfo(
                        "default", json.str("title"),
                        None, Set[String]( env.adminHost )
                    )
                } onFailMatch( recover.matcher {
                    case err@( _:nException | _:InvalidValueException )
                        => new InvalidData( err )
                })

                // Create a sample page
                page <- TryTo.except {
                    Page( site.id, "New Site", "home" )
                        .setIndex.setNavSort("10000")
                        .setContent( Seq( Markdown(
                            "New Site\n" +
                            "========\n\n" +
                            "Your new Shnappy site is ready!"
                        )))
                } onFailMatch ( recover.matchAll )

                // If there is already data, then block attempts to bootstrap
                _ <- TryTo.liftBool {
                    recover.fromFuture( data.hasData.map( !_ ) ).future
                } onFail {
                    recover.orRethrow( new Disallowed("Site already has data") )
                }

                // Save the data
                _ <- recover.fromFuture( Future.sequence( List(
                    data.save(user), data.save(site), data.save(page)
                ) ) )

            } {
                resp.json( nObject("status" -> "ok").toString ).done
            }
        })
    }
}

