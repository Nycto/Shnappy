package com.roundeights.shnappy

import scala.concurrent._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.Date

/**
 * A reference to a value produced by a Future
 */
private[shnappy] class LazyRef[T] (
    implicit val ctx: ExecutionContext
){

    /** The cached value */
    private val value = new AtomicReference[Future[T]]

    /** Returns this value */
    def apply( build: => Future[T] ): Future[T] = {

        def pull: Future[T] = {
            val pulled = build
            value.set( pulled )
            pulled
        }

        value.get match {
            case null => pull
            case saved => saved.recoverWith { case _ => pull }
        }
    }
}

/** @see LazyMap */
object LazyMap {

    /** A combined type that contains both the build and isFresh methods */
    trait Builder[V] {

        /** Fetches fresh data from source */
        def build: Future[V]

        /** Checks whether an existing cache value is fresh */
        def isFresh ( data: V ): Future[Boolean]
    }
}

/**
 * A lazy hash map
 */
private[shnappy] class LazyMap[K,V] ( implicit val ctx: ExecutionContext ) {

    /** A cache of page data by slug */
    private val cache = new ConcurrentHashMap[K, Future[V]]

    /** Returns a future reference for the given key */
    def get( key: K, callback: LazyMap.Builder[V] ): Future[V] = {

        def pull: Future[V] = {
            val pulled = callback.build
            cache.put( key, pulled )
            pulled
        }

        cache.get(key) match {
            case null => pull
            case defered => {
                defered.flatMap( value => {
                    callback.isFresh(value)
                        .recover { case _ => true }
                        .filter(fresh => fresh)
                        .map( _ => value )
                }).recoverWith { case _ => pull }
            }
        }
    }

    /** Returns a future reference for the given key */
    def get ( key: K, builder: () => Future[V] ): Future[V] = {
        get( key, new LazyMap.Builder[V] {
            override def build = builder()
            override def isFresh ( data: V ) = Future.successful(true)
        })
    }
}

