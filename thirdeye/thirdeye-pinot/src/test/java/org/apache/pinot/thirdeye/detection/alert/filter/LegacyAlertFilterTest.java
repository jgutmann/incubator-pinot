/**
 * Copyright (C) 2014-2018 LinkedIn Corp. (pinot-core@linkedin.com)
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

package org.apache.pinot.thirdeye.detection.alert.filter;

import org.apache.pinot.thirdeye.datalayer.dto.DetectionAlertConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.detection.DataProvider;
import org.apache.pinot.thirdeye.detection.MockDataProvider;
import org.apache.pinot.thirdeye.detection.alert.DetectionAlertFilterResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.apache.pinot.thirdeye.detection.DetectionTestUtils.*;


public class LegacyAlertFilterTest {
  private static final String PROP_DETECTION_CONFIG_IDS = "detectionConfigIds";
  private static final List<Long> PROP_ID_VALUE = Arrays.asList(1001L, 1002L);
  private static final String PROP_LEGACY_ALERT_FILTER_CONFIG = "legacyAlertFilterConfig";
  private static final String PROP_LEGACY_ALERT_FILTER_CLASS_NAME = "legacyAlertFilterClassName";
  private static final Set<String> TO_RECIPIENTS_VALUES = new HashSet<>(Arrays.asList("test@example.com", "mytest@example.org"));
  private static final Set<String> CC_RECIPIENTS_VALUES = new HashSet<>(Arrays.asList("iamcc@host.domain", "iamcc2@host.domain"));
  private static final Set<String> BCC_RECIPIENTS_VALUES = new HashSet<>(Arrays.asList("iambcc@host.domain"));
  private static final String PROP_RECIPIENTS = "recipients";

  private List<MergedAnomalyResultDTO> detectedAnomalies;
  private LegacyAlertFilter legacyAlertFilter;
  private LegacyAlertFilter legacyAlertFilterOnLegacyAnomalies;
  private Map<String, Set<String>> recipientsMap;

  @BeforeMethod
  public void beforeMethod() throws Exception {
    this.detectedAnomalies = new ArrayList<>();
    this.detectedAnomalies.add(makeAnomaly(1001L, 1500, 2000));
    this.detectedAnomalies.add(makeAnomaly(1001L, 0, 1000));
    this.detectedAnomalies.add(makeAnomaly(1002L, 0, 1000));
    this.detectedAnomalies.add(makeAnomaly(1002L, 1100, 1500));
    this.detectedAnomalies.add(makeAnomaly(1002L, 3333, 9999));
    this.detectedAnomalies.add(makeAnomaly(1003L, 1100, 1500));

    // Anomalies generated by legacy pipeline
    this.detectedAnomalies.add(makeAnomaly(null, 1000L, 1100, 1500));
    this.detectedAnomalies.add(makeAnomaly(null, 1002L, 0, 1000));
    this.detectedAnomalies.add(makeAnomaly(null, 1002L, 1100, 2000));


    DataProvider mockDataProvider = new MockDataProvider().setAnomalies(this.detectedAnomalies);

    DetectionAlertConfigDTO detectionAlertConfig = createDetectionAlertConfig();
    this.legacyAlertFilter = new LegacyAlertFilter(mockDataProvider, detectionAlertConfig, 2500L);

    DetectionAlertConfigDTO detectionAlertConfigLegacyAnomalies = createDetectionAlertConfig();
    detectionAlertConfigLegacyAnomalies.setOnlyFetchLegacyAnomalies(true);
    this.legacyAlertFilterOnLegacyAnomalies = new LegacyAlertFilter(mockDataProvider, detectionAlertConfigLegacyAnomalies, 2500L);

    this.recipientsMap = new HashMap<>();
    recipientsMap.put("to", TO_RECIPIENTS_VALUES);
    recipientsMap.put("cc", CC_RECIPIENTS_VALUES);
    recipientsMap.put("bcc", BCC_RECIPIENTS_VALUES);
  }

  private DetectionAlertConfigDTO createDetectionAlertConfig() {
    DetectionAlertConfigDTO detectionAlertConfig = new DetectionAlertConfigDTO();
    Map<String, Object> properties = new HashMap<>();
    properties.put(PROP_DETECTION_CONFIG_IDS, PROP_ID_VALUE);
    properties.put(PROP_LEGACY_ALERT_FILTER_CLASS_NAME, "org.apache.pinot.thirdeye.detector.email.filter.DummyAlertFilter");
    properties.put(PROP_LEGACY_ALERT_FILTER_CONFIG, "");
    properties.put(PROP_RECIPIENTS, recipientsMap);
    detectionAlertConfig.setHighWaterMark(0L);
    detectionAlertConfig.setProperties(properties);
    detectionAlertConfig.setVectorClocks(new HashMap<Long, Long>());

    return detectionAlertConfig;
  }

  @Test
  public void testRun() throws Exception {
    DetectionAlertFilterResult result = this.legacyAlertFilter.run();
    Assert.assertEquals(result.getAllAnomalies(), new HashSet<>(this.detectedAnomalies.subList(0, 4)));
  }

  @Test
  public void testFetchingLegacyAnomalies() throws Exception {
    DetectionAlertFilterResult result = this.legacyAlertFilterOnLegacyAnomalies.run();
    Assert.assertEquals(result.getAllAnomalies().size(), 2);
    Assert.assertEquals(result.getAllAnomalies(), new HashSet<>(this.detectedAnomalies.subList(7, 9)));
  }
}
