/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
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
package com.google.jenkins.plugins.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentialsModule;

/**
 * Tests for {@link GoogleRobotUsernamePasswordModule}.
 */
public class GoogleRobotUsernamePasswordModuleTest {

  /**
   */
  @NameWith(value = Namer.class, priority = 50)
  private abstract static class FakeGoogleRobotCredentials
      extends GoogleRobotCredentials {
    public FakeGoogleRobotCredentials(String a) {
      super(a, new GoogleRobotCredentialsModule());
    }
  }

  /**
   */
  public static class Namer
      extends CredentialsNameProvider<FakeGoogleRobotCredentials> {
    public String getName(FakeGoogleRobotCredentials c) {
      return NAME;
    }
  }

  @Mock
  private FakeGoogleRobotCredentials credentials;

  private GoogleCredential credential;

  private GoogleRobotUsernamePasswordModule underTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(credentials.getId()).thenReturn(CREDENTIALS_ID);
    when(credentials.forRemote(isA(GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credentials);

    credential = new GoogleCredential();
    when(credentials.getGoogleCredential(isA(
        GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(credential);

    when(credentials.getUsername()).thenReturn(USERNAME);
    underTest = new GoogleRobotUsernamePasswordModule(
        GoogleRobotUsernamePasswordModule.Strategy.CLOUD_PLATFORM);
  }

  @Test
  public void testIdentity() throws Exception {
    assertEquals(USERNAME, underTest.getIdentity(credentials));
  }

  @Test
  public void testForRemote() throws Exception {
    credential.setExpiresInSeconds(1000L);
    credential.setAccessToken(TOKEN);

    GoogleRobotUsernamePasswordModule remotable =
        underTest.forRemote(credentials);

    assertNotSame(remotable, underTest);
    assertEquals(remotable.getIdentity(credentials),
        underTest.getIdentity(credentials));
    assertEquals(remotable.getToken(credentials),
        underTest.getToken(credentials));
  }

  @Test
  public void testDoubleForRemote() throws Exception {
    GoogleRobotUsernamePasswordModule remotable =
        underTest.forRemote(credentials);

    assertNotSame(remotable, underTest);
    assertSame(remotable, remotable.forRemote(credentials));
  }

  private static final String CREDENTIALS_ID = "bazinga";
  private static final String NAME = "Source (foo.com:bar-baz)";
  private static final String USERNAME = "leeroy@google.com";
  private static final String TOKEN = "Th1s 1s M4 P4SsW0Rd!!!1!";
}