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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.MAX;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Date.from;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.lang3.Range.between;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.HttpConstants.DIGEST;
import static org.trellisldp.http.domain.HttpConstants.LINK_TEMPLATE;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.HttpConstants.RANGE;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.InvalidCardinality;
import static org.trellisldp.vocabulary.Trellis.InvalidRange;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractLdpResourceTest extends BaseLdpResourceTest {

    /* ****************************** *
     *           HEAD Tests
     * ****************************** */
    @Test
    public void testHeadDefaultType() {
        final Response res = target(RESOURCE_PATH).request().head();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    /* ******************************* *
     *            GET Tests
     * ******************************* */
    @Test
    public void testGetJson() throws IOException {
        final Response res = target("/" + RESOURCE_PATH).request().accept("application/ld+json").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH), "self")));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertTrue(res.hasEntity());

        final List<String> templates = res.getStringHeaders().get(LINK_TEMPLATE);
        assertEquals(2L, templates.size());
        assertTrue(templates.contains("<" + getBaseUrl() + RESOURCE_PATH + "{?subject,predicate,object}>; rel=\""
                + LDP.RDFSource.getIRIString() + "\""));
        assertTrue(templates.contains("<" + getBaseUrl() + RESOURCE_PATH + "{?version}>; rel=\""
                + Memento.Memento.getIRIString() + "\""));

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final List<Map<String, Object>> obj = MAPPER.readValue(entity,
                new TypeReference<List<Map<String, Object>>>(){});

        assertEquals(1L, obj.size());

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> titles = (List<Map<String, String>>) obj.get(0)
                .get(DC.title.getIRIString());

        final List<String> titleVals = titles.stream().map(x -> x.get("@value")).collect(toList());

        assertEquals(1L, titleVals.size());
        assertTrue(titleVals.contains("A title"));
    }

    @Test
    public void testGetDefaultType() {
        final Response res = target(RESOURCE_PATH).request().get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    @Test
    public void testGetDefaultType2() {
        final Response res = target("repository/resource").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    @Test
    public void testScrewyPreferHeader() {
        final Response res = target(RESOURCE_PATH).request().header("Prefer", "wait=just one minute").get();

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testScrewyAcceptDatetimeHeader() {
        final Response res = target(RESOURCE_PATH).request().header("Accept-Datetime",
                "it's pathetic how we both").get();

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testScrewyRange() {
        final Response res = target(BINARY_PATH).request().header("Range", "say it to my face, then").get();

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testGetRootSlash() {
        final Response res = target("/").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    @Test
    public void testGetRoot() {
        final Response res = target("").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    protected List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        return ofNullable(res.getStringHeaders().get(LINK)).orElseGet(Collections::emptyList)
            .stream().map(Link::valueOf).collect(toList());
    }

    @Test
    public void testGetDatetime() {
        assumeTrue(getBaseUrl().startsWith("http://localhost"));
        final Response res = target(RESOURCE_PATH).request()
            .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get();

        assertEquals(SC_OK, res.getStatus());
        assertNotNull(res.getHeaderString(MEMENTO_DATETIME));
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant());

        final List<Link> links = getLinks(res);
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")));
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000"), "self")));
        assertFalse(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH), "self")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testGetTrailingSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(from(time), res.getLastModified());
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
    }

    @Test
    public void testGetBinaryDescription() {
        final Response res = target(BINARY_PATH).request().accept("text/turtle").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertTrue(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), "self")));

        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));
    }

    @Test
    public void testGetBinary() throws IOException {
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")));
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), "self")));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertNotNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity);
    }

    @Test
    public void testGetBinaryMetadataError1() {
        when(mockBinary.getModified()).thenReturn(null);
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }

    @Test
    public void testGetBinaryMetadataError2() {
        when(mockBinary.getIdentifier()).thenReturn(null);
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }

    @Test
    public void testGetBinaryDigestInvalid() throws IOException {
        final Response res = target(BINARY_PATH).request().header(WANT_DIGEST, "FOO").get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertNotNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
        assertNull(res.getHeaderString(DIGEST));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity);
    }

    @Test
    public void testGetBinaryDigestMd5() throws IOException {
        final Response res = target(BINARY_PATH).request().header(WANT_DIGEST, "MD5").get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertNotNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
        assertEquals("md5=md5-digest", res.getHeaderString(DIGEST));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity);
    }

    @Test
    public void testGetBinaryDigestSha1() throws IOException {
        final Response res = target(BINARY_PATH).request().header(WANT_DIGEST, "SHA").get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertNotNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
        assertEquals("sha=sha1-digest", res.getHeaderString(DIGEST));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity);
    }

    @Test
    public void testGetBinaryRange() throws IOException {
        final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=3-10").get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertNotNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("e input", entity);
    }

    @Test
    public void testGetBinaryErrorSkip() throws IOException {
        when(mockBinaryService.getContent(eq(binaryInternalIdentifier))).thenReturn(of(mockInputStream));
        when(mockInputStream.skip(anyLong())).thenThrow(new IOException());
        final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=300-400").get();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }

    @Test
    public void testGetBinaryDigestError() throws IOException {
        when(mockBinaryService.getContent(eq(binaryInternalIdentifier))).thenReturn(of(mockInputStream));
        doThrow(new IOException()).when(mockInputStream).close();
        final Response res = target(BINARY_PATH).request().header(WANT_DIGEST, "MD5").get();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }

    @Test
    public void testGetBinaryError() {
        when(mockBinaryService.getContent(eq(binaryInternalIdentifier))).thenReturn(empty());
        final Response res = target(BINARY_PATH).request().get();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }

    @Test
    public void testGetVersionError() {
        final Response res = target(BINARY_PATH).queryParam("version", "looking at my history").request().get();
        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testGetVersionNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("version", "1496260729000").request().get();
        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testGetTimemapNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("ext", "timemap").request().get();
        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testGetTimegateNotFound() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .header(ACCEPT_DATETIME, "Wed, 16 May 2018 13:18:57 GMT").get();
        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testGetBinaryVersion() throws IOException {
        final Response res = target(BINARY_PATH).queryParam("version", timestamp).request().get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<Link> links = getLinks(res);
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + BINARY_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertEquals("bytes", res.getHeaderString(ACCEPT_RANGES));
        assertNotNull(res.getHeaderString(MEMENTO_DATETIME));
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant());

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertTrue(varies.contains(RANGE));
        assertTrue(varies.contains(WANT_DIGEST));
        assertFalse(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity);
    }

    @Test
    public void testPrefer() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + PreferServerManaged.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertTrue(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));

        assertEquals("A title", (String) obj.get("title"));
    }

    @Test
    public void testPrefer2() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child1")),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child2")),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child3")),
                rdf.createQuad(LDP.PreferMembership, identifier, LDP.member,
                    rdf.createIRI("trellis:data/resource/other")),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));

        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertTrue(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));
        assertFalse(obj.containsKey("contains"));
        assertFalse(obj.containsKey("member"));

        assertEquals("A title", (String) obj.get("title"));
    }

    @Test
    public void testPrefer3() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child1")),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child2")),
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                    rdf.createIRI("trellis:data/resource/child3")),
                rdf.createQuad(LDP.PreferMembership, identifier, LDP.member,
                    rdf.createIRI("trellis:data/resource/other")),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));

        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertFalse(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));
        assertTrue(obj.containsKey("contains"));
        assertTrue(obj.containsKey("member"));
    }

    @Test
    public void testGetJsonCompact() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(res.hasEntity());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertTrue(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));

        assertEquals("A title", (String) obj.get("title"));
    }

    @Test
    public void testGetJsonCompactLDF1() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.creator, rdf.createLiteral("User")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), DC.title, rdf.createIRI("ex:title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("Other title")),
                rdf.createQuad(PreferUserManaged, identifier, type, rdf.createIRI("ex:Type")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));

        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("predicate", "http://purl.org/dc/terms/title").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(res.hasEntity());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertFalse(obj.containsKey("creator"));
        assertTrue(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));

        assertTrue(obj.get("title") instanceof List);
        @SuppressWarnings("unchecked")
        final List<Object> titles = (List<Object>) obj.get("title");
        assertTrue(titles.contains("A title"));
        assertEquals(2L, titles.size());
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"));
    }

    @Test
    public void testGetJsonCompactLDF2() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.creator, rdf.createLiteral("User")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), DC.title, rdf.createIRI("ex:title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("Other title")),
                rdf.createQuad(PreferUserManaged, identifier, type, rdf.createIRI("ex:Type")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Type")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Other")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));

        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("object", "ex:Type").queryParam("predicate", "").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(res.hasEntity());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@type"));
        assertFalse(obj.containsKey("@context"));
        assertFalse(obj.containsKey("creator"));
        assertFalse(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));

        assertEquals("ex:Type", obj.get("@type"));
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"));
    }

    @Test
    public void testGetJsonCompactLDF3() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.creator, rdf.createLiteral("User")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), DC.title, rdf.createIRI("ex:title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("Other title")),
                rdf.createQuad(PreferUserManaged, identifier, type, rdf.createIRI("ex:Type")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Type")),
                rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Other")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));

        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("object", "A title").queryParam("predicate", DC.title.getIRIString()).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(res.hasEntity());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertFalse(obj.containsKey("@type"));
        assertTrue(obj.containsKey("@context"));
        assertFalse(obj.containsKey("creator"));
        assertTrue(obj.containsKey("title"));
        assertFalse(obj.containsKey("mode"));
        assertFalse(obj.containsKey("created"));

        assertEquals("A title", obj.get("title"));
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"));
    }


    @Test
    public void testGetTimeMapLinkDefaultFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType());
    }

    @Test
    public void testGetTimeMapLinkDefaultFormat2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target("repository/resource").queryParam("ext", "timemap").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType());
    }

    @Test
    public void testGetTimeMapLinkInvalidFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("some/made-up-format").get();

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus());
    }

    @Test
    public void testGetTimeMapLink() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.list(eq(identifier))).thenReturn(completedFuture(asList(
                between(ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000)),
                between(ofEpochSecond(timestamp - 1000), time),
                between(time, ofEpochSecond(timestamp + 1000)))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType());
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getLastModified());

        final List<Link> links = getLinks(res);
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final List<Link> entityLinks = stream(entity.split(",\n")).map(Link::valueOf).collect(toList());
        assertEquals(4L, entityLinks.size());
        entityLinks.forEach(l -> assertTrue(links.contains(l)));
    }

    @Test
    public void testGetTimeMapJsonCompact() throws IOException {
        when(mockMementoService.list(eq(identifier))).thenReturn(completedFuture(asList(
                between(ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000)),
                between(ofEpochSecond(timestamp - 1000), time),
                between(time, ofEpochSecond(timestamp + 1000)))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getLastModified());

        final List<Link> links = getLinks(res);

        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity,
                new TypeReference<Map<String, Object>>(){});

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> graph = (List<Map<String, Object>>) obj.get("@graph");

        assertEquals(5L, graph.size());
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH) &&
                    x.containsKey("timegate") && x.containsKey("timemap") && x.containsKey("memento")));
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                    x.containsKey("hasBeginning") &&
                    x.containsKey("hasEnd")));
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000") &&
                    x.containsKey("hasTime")));
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000") &&
                    x.containsKey("hasTime")));
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000") &&
                    x.containsKey("hasTime")));
    }

    @Test
    public void testGetTimeMapJson() throws IOException {
        when(mockMementoService.list(eq(identifier))).thenReturn(completedFuture(asList(
                between(ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000)),
                between(ofEpochSecond(timestamp - 1000), time),
                between(time, ofEpochSecond(timestamp + 1000)))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getLastModified());

        final List<Link> links = getLinks(res);

        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final List<Map<String, Object>> obj = MAPPER.readValue(entity,
                new TypeReference<List<Map<String, Object>>>(){});

        assertEquals(5L, obj.size());
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH) &&
                    x.containsKey("http://mementoweb.org/ns#timegate") &&
                    x.containsKey("http://mementoweb.org/ns#timemap") &&
                    x.containsKey("http://mementoweb.org/ns#memento")));
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                    x.containsKey("http://www.w3.org/2006/time#hasBeginning") &&
                    x.containsKey("http://www.w3.org/2006/time#hasEnd")));
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")));
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")));
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")));
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());

        final List<Link> links = getLinks(res);

        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(links.stream().anyMatch(hasType(LDP.Container)));

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant());
    }

    @Test
    public void testGetVersionContainerJson() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());

        final List<Link> links = getLinks(res);

        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 1000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("memento") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729000")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp + 1000))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap")));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timegate") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(l -> l.getRels().contains("original") &&
                    l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(links.stream().anyMatch(hasType(LDP.Container)));

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant());
    }

    @Test
    public void testGetNoAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().get();

        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testGetBinaryAcl() {
        when(mockBinaryResource.hasAcl()).thenReturn(true);
        final Response res = target(BINARY_PATH).queryParam("ext", "acl").request().get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")));
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")));
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")));
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")));
    }

    @Test
    public void testGetBinaryLinks() {
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_OK, res.getStatus());
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")));
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")));
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")));
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")));
    }

    @Test
    public void testGetBinaryDescriptionLinks() {
        final Response res = target(BINARY_PATH).request().accept("text/turtle").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")));
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")));
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")));
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")));
    }

    @Test
    public void testGetAclJsonCompact() throws IOException {
        when(mockResource.hasAcl()).thenReturn(true);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertEquals(from(time), res.getLastModified());
        // The next two assertions may change at some point
        assertFalse(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));
        assertFalse(getLinks(res).stream().anyMatch(l ->
                    l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + RESOURCE_PATH)));

        assertTrue(res.hasEntity());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.containsKey("@context"));
        assertFalse(obj.containsKey("title"));
        assertTrue(obj.containsKey("mode"));
        assertEquals(ACL.Control.getIRIString(), (String) obj.get("mode"));

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        final List<String> varies = res.getStringHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertFalse(varies.contains(PREFER));
    }

    @Test
    public void testGetLdpResource() {
        final Response res = target(RESOURCE_PATH).request().get();

        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    public void testGetNotFound() {
        final Response res = target(NON_EXISTENT_PATH).request().get();

        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testGetGone() {
        final Response res = target(DELETED_PATH).request().get();

        assertEquals(SC_GONE, res.getStatus());
    }

    @Test
    public void testGetCORSInvalid() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", "http://foo.com")
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));

        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    /* ******************************* *
     *            OPTIONS Tests
     * ******************************* */
    @Test
    public void testOptionsLDPRS() {
        final Response res = target(RESOURCE_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testOptionsLDPNR() {
        final Response res = target(BINARY_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));

        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testOptionsLDPC() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target(RESOURCE_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertTrue(res.getAllowedMethods().contains("POST"));

        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNotNull(res.getHeaderString(ACCEPT_POST));
        final List<String> acceptPost = asList(res.getHeaderString(ACCEPT_POST).split(","));
        assertEquals(3L, acceptPost.size());
        assertTrue(acceptPost.contains("text/turtle"));
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON));
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES));

        assertNull(res.getHeaderString(MEMENTO_DATETIME));

        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testOptionsACL() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testOptionsNonexistent() {
        final Response res = target(NON_EXISTENT_PATH).request().options();

        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testOptionsVersionNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("version", "1496260729000").request().options();
        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testOptionsGone() {
        final Response res = target(DELETED_PATH).request().options();

        assertEquals(SC_GONE, res.getStatus());
    }

    @Test
    public void testOptionsSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertTrue(res.getAllowedMethods().contains("PATCH"));
        assertTrue(res.getAllowedMethods().contains("PUT"));
        assertTrue(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testOptionsTimemap() {
        when(mockMementoService.list(identifier)).thenReturn(completedFuture(asList(
                between(ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000)),
                between(ofEpochSecond(timestamp - 1000), time),
                between(time, ofEpochSecond(timestamp + 1000)))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testOptionsVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus());

        assertFalse(res.getAllowedMethods().contains("PATCH"));
        assertFalse(res.getAllowedMethods().contains("PUT"));
        assertFalse(res.getAllowedMethods().contains("DELETE"));
        assertTrue(res.getAllowedMethods().contains("GET"));
        assertTrue(res.getAllowedMethods().contains("HEAD"));
        assertTrue(res.getAllowedMethods().contains("OPTIONS"));
        assertFalse(res.getAllowedMethods().contains("POST"));

        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
    }

    /* ******************************* *
     *            POST Tests
     * ******************************* */
    @Test
    public void testPost() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE)),
                    eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testPostRoot() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE)), eq(MAX)))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target("").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertEquals(getBaseUrl() + RANDOM_VALUE, res.getLocation().toString());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testPostInvalidLink() {
        final Response res = target(RESOURCE_PATH).request().header("Link", "I never really liked his friends")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostToTimemap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPostTypeMismatch() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(mockResource));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus());
    }

    @Test
    public void testPostUnknownLinkType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://example.com/types/Foo>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testPostBadContent() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostToLdpRs() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
                .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPostSlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header("Slug", "child")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
    }

    @Test
    public void testPostBadSlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header("Slug", "child/grandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().header("Slug", "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPostAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().header("Slug", "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPostConstraint() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://www.w3.org/ns/ldp#inbox> \"Some literal\" .",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPostIgnoreContains() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
    }

    @Test
    public void testPostNonexistent() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(NON_EXISTENT_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testPostGone() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + DELETED_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        final Response res = target(DELETED_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_GONE, res.getStatus());
    }

    @Test
    public void testPostBinary() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Slug", "newresource")
            .post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPostBinaryWithInvalidDigest() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Slug", "newresource")
            .header("Digest", "md5=blahblah").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostUnparseableDigest() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Digest", "digest this, man!").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostBinaryWithInvalidDigestType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Slug", "newresource")
            .header("Digest", "uh=huh").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPostBinaryWithMd5Digest() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Digest", "md5=BJozgIQwPzzVzSxvjQsWkA==")
            .header("Slug", "newresource").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPostBinaryWithSha1Digest() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Digest", "sha=3VWEuvPnAM6riDQJUu4TG7A4Ots=")
            .header("Slug", "newresource").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPostBinaryWithSha256Digest() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request()
            .header("Digest", "sha-256=voCCIRTNXosNlEgQ/7IuX5dFNvFQx5MfG/jy1AKiLMU=")
            .header("Slug", "newresource").post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPostTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPostSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().header("Slug", "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_OK, res.getStatus());
    }

    /* ******************************* *
     *            PUT Tests
     * ******************************* */
    @Test
    public void testPutExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutExistingBinaryDescription() {
        final Response res = target(BINARY_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutExistingUnknownLink() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://example.com/types/Foo>; rel=\"type\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }


    @Test
    public void testPutExistingIgnoreProperties() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" ;"
                        + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                        + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> .",
                        TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutExistingSubclassLink() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Link", LDP.Container + "; rel=\"type\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutExistingMalformed() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPutConstraint() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"Some literal\" .",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPutIgnoreContains() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testPutNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/test", res.getHeaderString(CONTENT_LOCATION));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutDeleted() {
        final Response res = target(DELETED_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertEquals(getBaseUrl() + DELETED_PATH, res.getHeaderString(CONTENT_LOCATION));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPutAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPutOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPutBinary() {
        final Response res = target(BINARY_PATH).request().put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutBinaryWithInvalidDigest() {
        final Response res = target(BINARY_PATH).request().header("Digest", "md5=blahblah")
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPutBinaryWithMd5Digest() {
        final Response res = target(BINARY_PATH).request().header("Digest", "md5=BJozgIQwPzzVzSxvjQsWkA==")
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutBinaryWithSha1Digest() {
        final Response res = target(BINARY_PATH).request().header("Digest", "sha=3VWEuvPnAM6riDQJUu4TG7A4Ots=")
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutBinaryToACL() {
        final Response res = target(BINARY_PATH).queryParam("ext", "acl").request()
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus());
    }

    @Test
    public void testPutIfMatch() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-Match", "\"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testPutBadIfMatch() {
        final Response res = target(BINARY_PATH).request().header("If-Match", "4db2c60044c906361ac212ae8684e8ad")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus());
    }

    @Test
    public void testPutIfUnmodified() {
        final Response res = target(BINARY_PATH).request()
            .header("If-Unmodified-Since", "Tue, 29 Aug 2017 07:14:52 GMT")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testPutPreconditionFailed() {
        final Response res = target(BINARY_PATH).request().header("If-Match", "\"blahblahblah\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus());
    }

    @Test
    public void testPutPreconditionFailed2() {
        final Response res = target(BINARY_PATH).request()
            .header("If-Unmodified-Since", "Wed, 19 Oct 2016 10:15:00 GMT")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus());
    }

    @Test
    public void testPutBinaryWithSha256Digest() {
        final Response res = target(BINARY_PATH).request()
            .header("Digest", "sha-256=voCCIRTNXosNlEgQ/7IuX5dFNvFQx5MfG/jy1AKiLMU=")
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPutSlash() {
        final Response res = target(RESOURCE_PATH + "/").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPutTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    /* ******************************* *
     *            DELETE Tests
     * ******************************* */
    @Test
    public void testDeleteExisting() {
        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testDeleteNonexistent() {
        final Response res = target(NON_EXISTENT_PATH).request().delete();

        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testDeleteDeleted() {
        final Response res = target(DELETED_PATH).request().delete();

        assertEquals(SC_GONE, res.getStatus());
    }

    @Test
    public void testDeleteVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().delete();

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testDeleteNonExistant() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request().delete();

        assertEquals(SC_NOT_FOUND, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testDeleteWithChildren() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.of(
                    rdf.createTriple(identifier, LDP.contains, rdf.createIRI(identifier.getIRIString() + "/child"))));

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testDeleteNoChildren1() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testDeleteNoChildren2() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
    }

    @Test
    public void testDeleteAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testDeleteTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().delete();
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testDeleteSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    /* ********************* *
     *      PATCH tests
     * ********************* */
    @Test
    public void testPatchVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPatchTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPatchExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchMissing() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));
        assertEquals(SC_NOT_FOUND, res.getStatus());
    }

    @Test
    public void testPatchGone() {
        final Response res = target(DELETED_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));
        assertEquals(SC_GONE, res.getStatus());
    }

    @Test
    public void testPatchExistingIgnoreLdpType() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" ;"
                        + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                        + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_OK, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertFalse(entity.contains("BasicContainer"));
        assertTrue(entity.contains("Type1"));
    }


    @Test
    public void testPatchExistingBinary() {
        final Response res = target(BINARY_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchExistingResponse() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_OK, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertTrue(entity.contains("A title"));
    }

    @Test
    public void testPatchConstraint() {
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> a \"Some literal\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPatchToTimemap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .method("PATCH", entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    @Test
    public void testPatchNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NOT_FOUND, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPatchOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus());
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testPatchAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPatchAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testPatchInvalidContent() {
        final Response res = target(RESOURCE_PATH).request().method("PATCH", entity("blah blah blah", "invalid/type"));

        assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchSlash() {
        final Response res = target(RESOURCE_PATH + "/").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(MEMENTO_DATETIME));
    }

    @Test
    public void testPatchNotAcceptable() {
        final Response res = target(RESOURCE_PATH).request().accept("text/foo")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus());
    }

    /**
     * Some other method
     */
    @Test
    public void testOtherMethod() {
        final Response res = target(RESOURCE_PATH).request().method("FOO");
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

    /* ************************************ *
     *      Test cache control headers
     * ************************************ */
    @Test
    public void testCacheControl() {
        final Response res = target(RESOURCE_PATH).request().get();
        assertEquals(SC_OK, res.getStatus());
        assertNotNull(res.getHeaderString(CACHE_CONTROL));
        assertTrue(res.getHeaderString(CACHE_CONTROL).contains("max-age="));
    }

    @Test
    public void testCacheControlOptions() {
        final Response res = target(RESOURCE_PATH).request().options();
        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString(CACHE_CONTROL));
    }

    protected static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    protected static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }
}
