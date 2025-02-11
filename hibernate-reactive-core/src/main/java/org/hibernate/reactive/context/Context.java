/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.context;

import org.hibernate.service.Service;

import java.util.concurrent.Executor;

/**
 * Abstracts away from the Vert.x {@link io.vertx.core.Context}
 * object, enabling alternative strategies for associating state
 * with the current reactive stream.
 *
 * @author Gavin King
 */
public interface Context extends Executor, Service {

    /**
     * Associate a value with the current reactive stream.
     */
    <T> void put(Key<T> key, T instance);

    /**
     * Get a value associated with the current reactive stream,
     * or return null.
     */
    <T> T get(Key<T> key);

    /**
     * Remove a value associated with the current reactive stream.
     */
    void remove(Key<?> key);

    /**
     * Run the given command in a context.
     *
     * If there is a context already associated with the call, use
     * that one. Otherwise, create a new context and execute the
     * command in the new context.
     */
    @Override
    void execute(Runnable runnable);

    interface Key<T> {

    }

}
