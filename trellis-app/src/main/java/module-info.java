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
module org.trellisldp.app {
    exports org.trellisldp.app;
    exports org.trellisldp.app.config;
    opens org.trellisldp.app.config to hibernate.validator;
    exports org.trellisldp.app.auth;
    exports org.trellisldp.app.health;
    requires dropwizard.auth;
    requires dropwizard.core;
    requires dropwizard.lifecycle;
    requires jackson.annotations;
    requires java.annotation;
    requires java.naming;
    requires java.ws.rs;
    requires javax.jms.api;
    requires java.xml.bind;
    requires jena.osgi;
    requires jena.tdb2;
    requires org.apache.commons.rdf.api;
    requires httpclient;
    requires httpclient.cache;
    requires httpcore;
    requires org.apache.servicemix.bundles.kafka.clients;
    requires org.trellisldp.agent;
    requires org.trellisldp.api;
    requires org.trellisldp.file;
    requires org.trellisldp.http;
    requires org.trellisldp.id;
    requires org.trellisldp.io;
    requires org.trellisldp.jms;
    requires org.trellisldp.kafka;
    requires org.trellisldp.namespaces;
    requires org.trellisldp.test;
    requires org.trellisldp.triplestore;
    requires org.trellisldp.vocabulary;
    requires org.trellisldp.webac;
    requires slf4j.api;
}