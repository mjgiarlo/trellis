/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.http;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * A {@link ContainerResponseFilter} that adds WebSub headers to all
 * {@code GET} responses.
 *
 * @see <a href="https://www.w3.org/TR/websub/">WebSub</a>
 */
@Priority(USER)
public class WebSubHeaderFilter implements ContainerResponseFilter {

    private final String hub;

    /**
     * Create a new WebSub Header Decorator.
     *
     * @param hub the location of the websub hub
     */
    public WebSubHeaderFilter(final String hub) {
        this.hub = hub;
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) throws IOException {
        if (req.getMethod().equals(GET) && SUCCESSFUL.equals(res.getStatusInfo().getFamily())) {
            res.getHeaders().add(LINK, fromUri(hub).rel("hub").build());
        }
    }
}
