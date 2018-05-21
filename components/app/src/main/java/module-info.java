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
module org.trellisldp.jpms.app {
    exports org.trellisldp.app;
    exports org.trellisldp.app.config;
    exports org.trellisldp.app.auth;
    opens org.trellisldp.app.config to hibernate.validator;
    requires com.fasterxml.jackson.annotation;
    requires dropwizard.auth;
    requires dropwizard.core;
    requires dropwizard.lifecycle;
    requires java.annotation;
    requires java.naming;
    requires java.ws.rs;
    requires jjwt;
    requires metrics.healthchecks;
    requires org.apache.commons.rdf.api;
    requires org.trellisldp.jpms.agent;
    requires org.trellisldp.jpms.api;
    requires org.trellisldp.jpms.http;
    requires org.trellisldp.jpms.vocabulary;
    requires org.trellisldp.jpms.webac;
    requires slf4j.api;
    requires validation.api;
}