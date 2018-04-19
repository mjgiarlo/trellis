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
module org.trellisldp.app.triplestore {
    exports org.trellisldp.app.triplestore;
    requires dropwizard.lifecycle;
    requires dropwizard.core;
    requires guava;
    requires javax.jms.api;
    requires jena.tdb2;
    requires metrics.healthchecks;
    requires org.apache.jena.rdfconnection;
    requires org.apache.jena.arq;
    requires org.apache.servicemix.bundles.kafka.clients;
    requires org.trellisldp.api;
    requires org.trellisldp.app;
    requires org.trellisldp.file;
    requires org.trellisldp.id;
    requires org.trellisldp.io;
    requires org.trellisldp.namespaces;
    requires org.trellisldp.kafka;
    requires org.trellisldp.triplestore;
    requires slf4j.api;
}