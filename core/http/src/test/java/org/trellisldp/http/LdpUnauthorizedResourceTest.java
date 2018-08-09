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

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static javax.ws.rs.core.SecurityContext.DIGEST_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE_TYPE;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.IRI;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LdpUnauthorizedResourceTest extends BaseLdpResourceTest {

    @Override
    public Application configure() {

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        // Junit runner doesn't seem to work very well with JerseyTest
        initMocks(this);

        final WebAcFilter webacFilter = new WebAcFilter(mockAccessControlService);
        webacFilter.setChallenges(asList(BASIC_AUTH, DIGEST_AUTH));

        final ResourceConfig config = new ResourceConfig();
        config.register(new LdpResource(mockBundler));
        config.register(new TestAuthenticationFilter("testUser", "group"));
        config.register(webacFilter);
        config.register(new CrossOriginResourceSharingFilter(asList(origin),
                    asList("PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Datetime"),
                    emptyList(), false, 0));
        return config;
    }

    @BeforeEach
    public void setUpMocks() {
        super.setUpMocks();
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> of(mockResource));
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(emptySet());
    }

    @Test
    public void testGetJson() {
        final Response res = target("/repo1/resource").request().accept("application/ld+json").get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDefaultType() {
        final Response res = target("repo1/resource").request().get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testTrailingSlash() {
        final Response res = target("repo1/resource/").request().get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testCORS() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final Response res = target("repo1/resource").request().header("Origin", origin).options();
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    public void testOptions1() {
        final Response res = target("repo1/resource").request().options();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testOptions2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target("repo1/resource").request().options();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetJsonCompact() {
        final Response res = target("repo1/resource").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetTimeMapLink() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetTimeMapJson() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target("repo1/resource").queryParam("version", 1496262729).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetAclJsonCompact() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPatch1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPatch2() {
        final Response res = target("repo1/resource").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPost1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPost2() {
        final Response res = target("repo1/resource").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPut1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPut2() {
        final Response res = target("repo1/resource").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request().delete();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete2() {
        final Response res = target("repo1/resource").request().delete();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete3() {
        final Response res = target("repo1/resource/").request().delete();

        assertEquals(SC_UNAUTHORIZED, res.getStatus());
        assertEquals(join(",", DIGEST_AUTH, BASIC_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }
}
