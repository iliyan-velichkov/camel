/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.file.stress;

import java.util.Random;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@Disabled("Manual test")
@DisabledOnOs(OS.WINDOWS)
public class FileAsyncStressManualTest extends ContextTestSupport {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        int files = 150;
        for (int i = 0; i < files; i++) {
            template.sendBodyAndHeader(fileUri(), "Hello World", Exchange.FILE_NAME, i + ".txt");
        }
    }

    @Test
    public void testAsyncStress() throws Exception {
        // start route when all the files have been written
        context.getRouteController().startRoute("foo");

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(100);
        mock.setResultWaitTime(30000);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // leverage the fact that we can limit to max 50 files per poll
                // this will result in polling again and potentially picking up
                // files
                // that already are in progress
                from(fileUri("?maxMessagesPerPoll=50")).routeId("foo").autoStartup(false).threads(10)
                        .process(new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                // simulate some work with random time to complete
                                Random ran = new Random();
                                int delay = ran.nextInt(50) + 10;
                                Thread.sleep(delay);
                            }
                        }).to("mock:result");
            }
        };
    }

}
