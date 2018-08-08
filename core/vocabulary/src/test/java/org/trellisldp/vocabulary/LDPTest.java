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
package org.trellisldp.vocabulary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Test the LDP Vocabulary Class
 * @author acoburn
 */
public class LDPTest extends AbstractVocabularyTest {

    @Override
    public String namespace() {
        return "http://www.w3.org/ns/ldp#";
    }

    @Override
    public Class<LDP> vocabulary() {
        return LDP.class;
    }

    @Test
    public void testSuperclass() {
        assertEquals(LDP.Resource, LDP.getSuperclassOf(LDP.NonRDFSource));
        assertEquals(LDP.Container, LDP.getSuperclassOf(LDP.BasicContainer));
        assertNull(LDP.getSuperclassOf(LDP.Resource));
    }
}
