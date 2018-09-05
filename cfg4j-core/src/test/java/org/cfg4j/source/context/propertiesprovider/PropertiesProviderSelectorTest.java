/*
 * Copyright 2015-2016 Norbert Potocki (norbert.potocki@nort.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cfg4j.source.context.propertiesprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Properties;


@ExtendWith(MockitoExtension.class)
class PropertiesProviderSelectorTest {




  @Mock
  private PropertiesProvider yamlProvider;
  private Properties yamlProperties;

  @Mock
  private PropertiesProvider jsonProvider;
  private Properties jsonProperties;

  @Mock
  private PropertiesProvider propertiesProvider;
  private Properties propertiesProperties;

  private PropertiesProviderSelector selector;

  @BeforeEach
  public void setUp() {
    yamlProperties = new Properties();
    when(yamlProvider.getProperties(any(InputStream.class))).thenReturn(yamlProperties);

    jsonProperties = new Properties();
    when(jsonProvider.getProperties(any(InputStream.class))).thenReturn(jsonProperties);

    propertiesProperties = new Properties();
    when(yamlProvider.getProperties(any(InputStream.class))).thenReturn(propertiesProperties);

    selector = new PropertiesProviderSelector(propertiesProvider, yamlProvider, jsonProvider);
  }

  @Test
  public void returnsYamlProviderForYaml() {
    assertThat(selector.getProvider("test.yaml")).isEqualTo(yamlProvider);
  }

  @Test
  public void returnsYamlProviderForYml() {
    assertThat(selector.getProvider("test.yml")).isEqualTo(yamlProvider);
  }

  @Test
  public void returnsJsonProviderForJson() {
    assertThat(selector.getProvider("test.json")).isEqualTo(jsonProvider);
  }

  @Test
  public void returnsPropertiesProviderForNonYaml() {
    assertThat(selector.getProvider("test.properties")).isEqualTo(propertiesProvider);
  }
}