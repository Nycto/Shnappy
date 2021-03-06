package com.roundeights.shnappy.admin

import scala.concurrent.ExecutionContext.Implicits.global
import com.roundeights.foldout.Documentable
import com.roundeights.shnappy.component.{Parser, Markdown}
import com.roundeights.shnappy._
import com.roundeights.tubeutil.BodyData
import com.roundeights.skene._
import com.roundeights.scalon._
import com.roundeights.attempt._
import com.roundeights.vfunk.InvalidValueException
import java.util.UUID

/**
 * Page API handlers
 */
class ContentApiHandler (
    val req: Registry, val data: AdminData, val parser: Parser
) extends Skene {

    // A common type for content objects
    type Content = Documentable with nElement.ToJson

    // Returns all the content for a specific site
    get("/admin/api/sites/:siteID/content")(
        req.use[SiteEditor].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getPagesAndLinks( prereqs.siteID )
            ).onSuccess {
                case content => resp.json( nElement( content.map {
                    case Left(page) => page.toJsonLite
                    case Right(link) => link.toJson
                } ).toString ).done
            }
        })
    )

    // Returns the index page for a site
    get("/admin/api/sites/:siteID/index")(
        req.use[SiteEditor].in((prereqs, resp, recover) => {
            recover.fromFuture(
                data.getIndex( prereqs.siteID )
            ).onSuccess {
                case None => throw new NotFound("No index for site")
                case Some(page) => resp.json( page.toJson.toString ).done
            }
        })
    )

    // Creates a new piece of content
    post("/admin/api/sites/:siteID/content")(
        req.use[
            SiteEditor, SiteParam, BodyData
        ].in((prereqs, resp, recover) => {
            val json = prereqs.json

            for {
                typename <- json.str_?("type") :: OnFail {
                    recover.orRethrow(
                        new InvalidData( "Missing a valid type parameter" )
                    )
                }

                content: Content <- TryTo.except {
                    typename match {
                        case "page" => Page(prereqs.siteParam, parser, json)
                        case "link" => RawLink(prereqs.siteParam, json)
                        case _ => throw new InvalidData("Invalid content type")
                    }
                } onFailMatch( recover.matcher {
                    case err@( _:nException | _:InvalidValueException )
                        => new InvalidData( err )
                } )

                _ <- recover.fromFuture( data.save(content) )

            } resp.json( content.toJson.toString ).done
        })
    )

    // Returns a specific piece of content
    get("/admin/api/content/:contentID")(
        req.use[ContentEditor, ContentParam].in((prereqs, resp, recover) => {
            resp.json( prereqs.contentParam match {
                case Left(page) => page.toJson.toString
                case Right(link) => link.toJson.toString
            }).done
        })
    )

    // Updates a specific piece of content
    patch("/admin/api/content/:contentID")(
        req.use[
            ContentEditor, BodyData, ContentParam
        ].in((prereqs, resp, recover) => for {

            updated: Content <- TryTo.except {
                prereqs.contentParam match {

                    case Left(page) => {
                        prereqs.json.patch( page )
                            .patch[String]("title", _ withTitle _)
                            .patch[String]("slug", _ withSlug _)
                            .patchElem("index", _ setIndex _ )
                            .patchElem("navSort", _ setNavSort _ )
                            .patchElem("content", (page, content) => {
                                page.setContent( parser.parse( content ) )
                            })
                            .done
                    }

                    case Right(link) => {
                        prereqs.json.patch( link )
                            .patch[String]("url", _ withURL _)
                            .patch[String]("text", _ withText _)
                            .patchElem("navSort", (link, sort) => {
                                link.withSort( sort.asString )
                            })
                            .done
                    }

                }
            } onFailMatch ( recover.matcher {
                case err@( _:nException | _:InvalidValueException )
                    => new InvalidData( err )
            })

            _ <- recover.fromFuture( data.save(updated) )

        } resp.json( updated.toJson.toString ).done )
    )

    // Deletes a specific piece of content
    delete("/admin/api/content/:contentID")(
        req.use[
            ContentEditor, BodyData, ContentParam
        ].in((prereqs, resp, recover) => {

            recover.fromFuture( data.delete(
                prereqs.contentParam match {
                    case Left(doc) => doc
                    case Right(doc) => doc
                }
            ) ).onSuccess {
                case _ => resp.json( nObject("status" -> "ok").toString ).done
            }
        })
    )
}

