/**
 * Copyright (C) 2016 The Weghst Inc. <kevinz@weghst.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weghst.setaria.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.weghst.setaria.client.annotation.ConfigValue;

import static org.testng.Assert.*;

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@ContextConfiguration(classes = SpringConfigurer.class)
@TestExecutionListeners(listeners = SetarConfigTestExecutionListener.class)
public class FileSetariaConfigTest extends AbstractTestNGSpringContextTests {

    private static final Path PROPERTIES_PATH = Paths.get(System.getProperty("java.io.tmpdir"),
            "test-fileSetariaConfig.properties");

    private SetariaConfig setariaConfig;

    @Autowired
    private TestConfigBean testConfigBean;

    @BeforeSuite
    public void beforeSuite() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("test.first", "Test First");
        properties.setProperty("test.second", "Test Second");

        storeProperties(properties);

        Map<String, String> configParameters = new HashMap<>();
        configParameters.put(FileSetariaConfig.SETARIA_CONFIG_LOCATION, PROPERTIES_PATH.toString());

        setariaConfig = new FileSetariaConfig(configParameters);
        setariaConfig.init();
        SetariaConfigContext.setSetariaConfig(setariaConfig);
    }

    @Test
    public void testRefresh() throws IOException {
        assertEquals(Configs.getString("test.first"), testConfigBean.first);
        assertEquals("Default Value", testConfigBean.third);

        Properties properties = new Properties();
        properties.setProperty("test.first", "Test First New Value");
        properties.setProperty("test.second", "Test Second");
        storeProperties(properties);

        setariaConfig.refresh();

        assertEquals(properties.getProperty("test.first"), testConfigBean.first);
        assertEquals(Configs.getString("test.first"), testConfigBean.first);
        assertEquals("Default Value", testConfigBean.third);
    }

    private void storeProperties(Properties properties) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(PROPERTIES_PATH.toFile())) {
            properties.store(outputStream, FileSetariaConfigTest.class.getName());
        }
    }

    @Component
    public static class TestConfigBean {

        @ConfigValue("${test.first}")
        private String first;
        private String second;
        @ConfigValue("${test.third:Default Value}")
        private String third;

        @ConfigValue("${test.second:HELLO}")
        public void setSecond(String second) {
            this.second = second;
        }
    }

}