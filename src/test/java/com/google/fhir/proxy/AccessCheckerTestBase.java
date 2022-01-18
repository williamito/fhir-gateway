/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.fhir.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

// This base test class tries to strike a [subjective] balance between DRY and DAMP: go/tott/598
@RunWith(MockitoJUnitRunner.class)
public abstract class AccessCheckerTestBase {
  static final String PATIENT_AUTHORIZED = "be92a43f-de46-affa-b131-bbf9eea51140";
  static final String PATIENT_NON_AUTHORIZED = "patient-non-authorized";
  static final IdDt PATIENT_AUTHORIZED_ID = new IdDt("Patient", PATIENT_AUTHORIZED);
  static final IdDt PATIENT_NON_AUTHORIZED_ID = new IdDt("Patient", PATIENT_NON_AUTHORIZED);

  @Mock protected RestfulServer serverMock;

  @Mock protected DecodedJWT jwtMock;

  @Mock protected Claim claimMock;

  @Mock protected RequestDetails requestMock;

  // Note this is an expensive class to instantiate, so we only do this once for all tests.
  protected static final FhirContext fhirContext = FhirContext.forR4();

  protected abstract AccessChecker getInstance(RestfulServer server);

  @Test
  public void createTest() {
    AccessChecker testInstance = getInstance(serverMock);
  }

  @Test
  public void canAccessTest() {
    when(requestMock.getResourceName()).thenReturn("Patient");
    when(requestMock.getId()).thenReturn(PATIENT_AUTHORIZED_ID);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(true));
  }

  @Test
  public void canAccessNotAuthorized() {
    when(requestMock.getResourceName()).thenReturn("Patient");
    when(requestMock.getId()).thenReturn(PATIENT_NON_AUTHORIZED_ID);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }

  @Test
  public void canAccessSearchQuery() {
    when(requestMock.getResourceName()).thenReturn("Observation");
    Map<String, String[]> params = Maps.newHashMap();
    params.put("subject", new String[] {PATIENT_AUTHORIZED});
    when(requestMock.getParameters()).thenReturn(params);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(true));
  }

  @Test
  public void canAccessSearchQueryNotAuthorized() {
    when(requestMock.getResourceName()).thenReturn("Observation");
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }

  @Test
  public void canAccessPutObservation() throws IOException {
    when(requestMock.getResourceName()).thenReturn("Observation");
    URL listUrl = Resources.getResource("test_obs.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(true));
  }

  @Test
  public void canAccessPutObservationUnauthorized() throws IOException {
    when(requestMock.getResourceName()).thenReturn("Observation");
    URL listUrl = Resources.getResource("test_obs_unauthorized.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }

  @Test
  public void canAccessPostObservation() throws IOException {
    when(requestMock.getResourceName()).thenReturn("Observation");
    URL listUrl = Resources.getResource("test_obs.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.POST);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(true));
  }

  @Test
  public void canAccessPostObservationWithPerformer() throws IOException {
    // The sample Observation resource below has a few `performers` references in it. This is to see
    // if the `Patient` performer references are properly extracted and passed to the List query.
    when(requestMock.getResourceName()).thenReturn("Observation");
    URL listUrl = Resources.getResource("test_obs_performers.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.POST);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(true));
  }

  @Test
  public void canAccessPostObservationNoSubject() throws IOException {
    when(requestMock.getResourceName()).thenReturn("Observation");
    URL listUrl = Resources.getResource("test_obs_no_subject.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.POST);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }

  @Test
  public void canAccessPostWrongQueryPath() throws IOException {
    when(requestMock.getResourceName()).thenReturn("Encounter");
    URL listUrl = Resources.getResource("test_obs.json");
    byte[] obsBytes = Resources.toByteArray(listUrl);
    when(requestMock.loadRequestContents()).thenReturn(obsBytes);
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.POST);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }

  @Test
  public void canAccessPutPatientNoId() {
    when(requestMock.getResourceName()).thenReturn("Patient");
    when(requestMock.getRequestType()).thenReturn(RequestTypeEnum.PUT);
    when(requestMock.getId()).thenReturn(null);
    AccessChecker testInstance = getInstance(serverMock);
    assertThat(testInstance.checkAccess(requestMock).canAccess(), equalTo(false));
  }
}