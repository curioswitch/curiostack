/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.common.server.framework.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * {@link Qualifier} to use on {@link Object} type providers to indicate the provided objects should
 * be initialized at server startup. This is useful for dependencies used in {@link
 * dagger.producers.ProducerModule} because they will not be initialized until the first usage of a
 * producer graph. For {@link javax.inject.Singleton} dependencies, it is highly recommended to
 * provide an {@link EagerInit} annotated {@code init} provider that accepts the objects as
 * dependencies.
 *
 * <p>For example,
 *
 * <pre>{@code
 * {@literal @}Module
 * public class MyModule {
 *   {@literal @}Provides
 *   {@literal @}Singleton
 *   public Database slowDatabase() {
 *     return Database.connect();
 *   }
 *
 *   {@literal @}Provides
 *   {@literal @}Singleton
 *   public ServerConfig serverConfig(Config config) {
 *     return config.load("server");
 *   }
 *
 *   {@literal @Provides}
 *   {@literal @ElementsIntoSet}
 *   {@literal @EagerInit}
 *   public Set<Object> init(Database database, ServerConfig config) {
 *     return ImmutableSet.of(database, config);
 *   }
 * }
 * }</pre>
 */
@Qualifier
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface EagerInit {}
