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
module org.trellisldp.http {
    exports org.trellisldp.http;
    exports org.trellisldp.http.domain;
    exports org.trellisldp.http.impl;
    opens org.trellisldp.http.domain to hk2.utils;
    requires java.annotation;
    requires java.json;
    requires java.ws.rs;
    requires javax.inject;
    requires org.apache.commons.codec;
    requires org.apache.commons.rdf.api;
    requires org.trellisldp.api;
    requires org.trellisldp.vocabulary;
    requires slf4j.api;
    uses org.trellisldp.api.ConstraintService;
    uses org.apache.commons.rdf.api.RDF;
}