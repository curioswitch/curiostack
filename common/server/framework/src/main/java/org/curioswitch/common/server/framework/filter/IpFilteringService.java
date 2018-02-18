/**
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
package org.curioswitch.common.server.framework.filter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.net.HttpHeaders;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.Service;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingService;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link com.linecorp.armeria.server.DecoratingService} which only allows requests from a whitelist
 * of IP addresses. This can be used to, for example, restrict requests to a corporate office. IP
 * restrictions tend to be difficult to work with, especially with remote workers, and hard to
 * manage - when possible, an authentication based system like Google Identity-Aware Proxy is
 * strongly recommended.
 */
public class IpFilteringService extends SimpleDecoratingService<HttpRequest, HttpResponse> {

  private static final Logger logger = LogManager.getLogger();

  private static final Splitter RULE_SPLITTER = Splitter.on('/');

  public static Function<Service<HttpRequest, HttpResponse>, IpFilteringService> newDecorator(
      List<String> ipRules) {
    return service -> new IpFilteringService(service, ipRules);
  }

  private final List<IpFilterRule> rules;

  /** Creates a new instance that decorates the specified {@link Service}. */
  private IpFilteringService(Service<HttpRequest, HttpResponse> delegate, List<String> ipRules) {
    super(delegate);
    rules = parseRules(ipRules);
  }

  @Override
  public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
    String xForwardedFor = req.headers().get(HttpHeaderNames.of(HttpHeaders.X_FORWARDED_FOR));
    InetSocketAddress remoteAddress = ctx.remoteAddress();
    final InetSocketAddress clientAddress;
    if (xForwardedFor != null) {
      int commaIndex = xForwardedFor.indexOf(',');
      String clientIp = commaIndex < 0 ? xForwardedFor : xForwardedFor.substring(0, commaIndex);
      clientAddress = new InetSocketAddress(clientIp, remoteAddress.getPort());
    } else {
      clientAddress = remoteAddress;
    }
    if (rules.stream().anyMatch(rule -> rule.matches(clientAddress))) {
      return delegate().serve(ctx, req);
    } else {
      logger.info("Denying access from IP " + clientAddress);
      return HttpResponse.of(HttpStatus.FORBIDDEN);
    }
  }

  private static List<IpFilterRule> parseRules(List<String> ipRules) {
    return ipRules
        .stream()
        .map(
            rule -> {
              List<String> parts = RULE_SPLITTER.splitToList(rule);
              // TODO(choko): Add better validation.
              checkArgument(parts.size() == 2, "invalid rule: {}", rule);
              return new IpSubnetFilterRule(
                  parts.get(0), Integer.parseInt(parts.get(1)), IpFilterRuleType.ACCEPT);
            })
        .collect(toImmutableList());
  }
}
