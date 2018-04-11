/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

package org.curioswitch.curiostack.gateway;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.linecorp.armeria.client.ClientOptions;
import com.linecorp.armeria.server.Service;
import java.net.URI;
import java.util.List;
import org.curioswitch.common.server.framework.immutables.CurioStyle;
import org.immutables.value.Value.Immutable;

/**
 * The routing configuration for the gateway.
 */
@Immutable
@CurioStyle
@JsonDeserialize(as = ImmutableRoutingConfig.class)
interface RoutingConfig {

  /**
   * A routing target, corresponding to a single backend.
   */
  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableTarget.class)
  interface Target {

    /**
     * A descriptive name for the target. Should only consistent of letters, underscores, and
     * hyphens.
     */
    String getName();

    /**
     * URL of the backend. Any URL that can be recognized by
     * {@link com.linecorp.armeria.client.Clients#newClient(URI, Class, ClientOptions)} can be
     * specified. DNS load balancing will be enabled for the URL.
     */
    String getUrl();
  }

  /**
   * The targets to route to. Rules should be specified below using the names of these targets.
   */
  List<Target> getTargets();

  /** A single routing rule for the gateway. Each rule corresponds to a backend server. */
  @Immutable
  @CurioStyle
  @JsonDeserialize(as = ImmutableRule.class)
  interface Rule {

    /**
     * The incoming path pattern of requests to route to this backend. Any path pattern supported by
     * {@link com.linecorp.armeria.server.PathMapping#of(String)} is supported (most users will use
     * the format 'prefix:/foobar' the most).
     *
     * @see com.linecorp.armeria.server.ServerBuilder#service(String, Service)
     */
    String getPathPattern();

    /**
     * The name of the target to route requests to the path to.
     */
    String getTarget();
  }

  /**
   * Get the routing config's rules.
   */
  List<Rule> getRules();
}
