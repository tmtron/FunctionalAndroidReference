/*
 * Copyright (c) pakoito 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacoworks.dereference.features.cache

import com.pacoworks.dereference.architecture.ui.createStateHolder
import com.pacoworks.dereference.features.cache.model.createKnownCharacter
import com.pacoworks.dereference.features.cache.model.createUnknownIncorrectCharacter
import com.pacoworks.dereference.features.cache.model.createUnknownNetworkErrorCharacter
import com.pacoworks.dereference.features.cache.model.createUnknownUnavailableCharacter
import com.pacoworks.dereference.features.cache.services.CacheRequest
import org.junit.Assert.assertTrue
import org.junit.Test
import rx.Observable

class CacheExampleSubscribeCacheTest {

    @Test
    fun unavailable_requestCharacter_serverSuccess_availableCharacter() {
        val newId = "newChar"
        val initialId = "initialId"
        val filterValue = createStateHolder(initialId)
        val characterCache = createStateHolder(
                mapOf(initialId to createUnknownUnavailableCharacter(initialId),
                        newId to createUnknownUnavailableCharacter(newId)))
        val currentCharacter = createStateHolder(createUnknownUnavailableCharacter(""))
        val state = CacheExampleState(characterCache = characterCache, currentCharacter = currentCharacter)
        /* Server returns correctly */
        val server: CacheRequest = { Observable.just(createKnownCharacter(it, it, listOf())) }
        /* Start subscription */
        handleFilterChange(server, state, filterValue)
        /* Assert initial state as unavailable */
        assertTrue(characterCache.toBlocking().first()[newId]!!
                .join(
                        { unavailable ->
                            unavailable.reason.join(
                                    { notRequested -> true },
                                    { serverError -> false },
                                    { invalid -> false })
                        }, { available -> false }))
        /* Call for characterId */
        filterValue.call(newId)
        /* Check state */
        assertTrue(characterCache.toBlocking().first()[newId]!!.join({ unavailable -> false }, { available -> true }))
    }

    @Test
    fun unavailable_requestCharacter_serverFailure_errorCharacter() {
        val newId = "newChar"
        val initialId = "initialId"
        val filterValue = createStateHolder(initialId)
        val characterCache = createStateHolder(
                mapOf(initialId to createUnknownUnavailableCharacter(initialId),
                        newId to createUnknownUnavailableCharacter(newId)))
        val currentCharacter = createStateHolder(createUnknownUnavailableCharacter(""))
        val state = CacheExampleState(characterCache = characterCache, currentCharacter = currentCharacter)
        /* Server returns a problem */
        val server: CacheRequest = { Observable.just(createUnknownNetworkErrorCharacter("Explosion")) }
        /* Start subscription */
        handleFilterChange(server, state, filterValue)
        /* Assert initial state as unavailable */
        assertTrue(characterCache.toBlocking().first()[newId]!!.join({ unavailable -> true }, { available -> false }))
        /* Call for characterId */
        filterValue.call(newId)
        /* Check state */
        assertTrue(characterCache.toBlocking().first()[newId]!!
                .join(
                        { unavailable ->
                            unavailable.reason.join(
                                    { notRequested -> false },
                                    { serverError -> true },
                                    { invalid -> false })
                        }, { available -> false }))
    }

    @Test
    fun unavailable_requestCharacter_serverInvalidResult_invalidCharacter() {
        val newId = "newChar"
        val initialId = "initialId"
        val filterValue = createStateHolder(initialId)
        val characterCache = createStateHolder(
                mapOf(initialId to createUnknownUnavailableCharacter(initialId),
                        newId to createUnknownUnavailableCharacter(newId)))
        val currentCharacter = createStateHolder(createUnknownUnavailableCharacter(""))
        val state = CacheExampleState(characterCache = characterCache, currentCharacter = currentCharacter)
        /* Server returns incorrect character */
        val server: CacheRequest = { Observable.just(createUnknownIncorrectCharacter(it)) }
        /* Start subscription */
        handleFilterChange(server, state, filterValue)
        /* Assert initial state as unavailable */
        assertTrue(characterCache.toBlocking().first()[newId]!!.join({ unavailable -> true }, { available -> false }))
        /* Call for characterId */
        filterValue.call(newId)
        /* Check state */
        assertTrue(characterCache.toBlocking().first()[newId]!!
                .join(
                        { unavailable ->
                            unavailable.reason.join(
                                    { notRequested -> false },
                                    { serverError -> false },
                                    { invalid -> true })
                        }, { available -> false }))
    }

    @Test
    fun available_requestCharacter_noServerCall() {
        val newId = "newChar"
        val initialId = "initialId"
        val filterValue = createStateHolder(initialId)
        /* Character starts as available */
        val characterCache = createStateHolder(mapOf(initialId to createKnownCharacter(initialId, initialId, listOf()),
                newId to createKnownCharacter(newId, newId, listOf())))
        val currentCharacter = createStateHolder(createUnknownUnavailableCharacter(""))
        val state = CacheExampleState(characterCache = characterCache, currentCharacter = currentCharacter)
        /* Server fails to signal it's been called */
        val server: CacheRequest = { Observable.error(RuntimeException("Should not be called")) }
        /* Start subscription */
        handleFilterChange(server, state, filterValue)
        /* Assert initial state as unavailable */
        assertTrue(characterCache.toBlocking().first()[newId]!!.join({ unavailable -> false }, { available -> true }))
        /* Call for characterId */
        filterValue.call(newId)
        /* Check state */
        assertTrue(characterCache.toBlocking().first()[newId]!!.join({ unavailable -> false }, { available -> true }))
    }
}