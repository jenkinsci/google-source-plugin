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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentialsModule;
import com.thoughtworks.xstream.XStream;

import hudson.util.Secret;

/**
 * Tests for {@link GoogleRobotUsernamePassword}.
 */
public class GoogleRobotUsernamePasswordTest {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

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

  private GoogleRobotUsernamePassword underTest;

  private static class FakeModule extends GoogleRobotUsernamePasswordModule {

    public FakeModule(Strategy strategy) {
      super(strategy);
    }
    @Override
    public String getIdentity(GoogleRobotCredentials credentials) {
      return USERNAME;
    }

    @Override
    public GoogleRobotUsernamePasswordModule forRemote(
        GoogleRobotCredentials credentials) {
      return new OtherFakeModule(getStrategy());
    }
  }

  private static class OtherFakeModule
      extends GoogleRobotUsernamePasswordModule {
    public OtherFakeModule(Strategy strategy) {
      super(strategy);
    }

    public String getIdentity(GoogleRobotCredentials credentials) {
      return USERNAME;
    }

    @Override
    public GoogleRobotUsernamePasswordModule forRemote(
        GoogleRobotCredentials credentials) {
      return this;
    }
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(credentials.getId()).thenReturn(CREDENTIALS_ID);
    if (jenkins.jenkins != null) {
      SystemCredentialsProvider.getInstance().getCredentials().add(
          credentials);
    }

    underTest = new GoogleRobotUsernamePassword(CREDENTIALS_ID,
        new FakeModule(
            GoogleRobotUsernamePasswordModule.Strategy.CLOUD_PLATFORM));
  }

  @Test
  public void testDescription() throws Exception {
    assertEquals(NAME, underTest.getDescription());
  }

  @Test
  public void testUsername() throws Exception {
    assertEquals(USERNAME, underTest.getUsername());
  }

  @Test
  public void testPassword() throws Exception {
    when(credentials.getAccessToken(isA(GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(Secret.fromString(PASSWORD));

    assertEquals(PASSWORD, Secret.toString(underTest.getPassword()));
  }

  @Test
  public void testNameProvider() {
    assertThat(CredentialsNameProvider.name(underTest),
        containsString(NAME));
  }

  @Test
  public void testWriteObject() throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    assertThat(xml, containsString("-OtherFakeModule"));
  }

  @Test
  public void testWriteObjectThenReadObjectSameModule() throws Exception {
    underTest = new GoogleRobotUsernamePassword(CREDENTIALS_ID,
        new OtherFakeModule(
            GoogleRobotUsernamePasswordModule.Strategy.CLOUD_PLATFORM));

    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);
    System.out.println(xml);

    GoogleRobotUsernamePassword deserialized =
        (GoogleRobotUsernamePassword) xstream.fromXML(xml);
    String reserializedXml = xstream.toXML(deserialized);
    System.out.println(reserializedXml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof OtherFakeModule);
    assertTrue(deserialized.module instanceof OtherFakeModule);
    assertEquals(underTest.getId(), deserialized.getId());
    assertEquals(underTest.getCredentialsId(), deserialized.getCredentialsId());
    assertEquals(xml, reserializedXml);
  }

  @Test
  public void testWriteObjectThenReadObjectModuleChangeButOnMaster()
      throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    // Contains OtherFakeModule, in case it is
    // deserialized remotely.
    assertThat(xml, containsString("-OtherFakeModule"));

    GoogleRobotUsernamePassword deserialized =
        (GoogleRobotUsernamePassword) xstream.fromXML(xml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof FakeModule);
    // With JenkinsRule, we are still a FakeModule
    assertTrue(deserialized.module instanceof FakeModule);
  }

  @Test
  @WithoutJenkins
  public void testWriteObjectThenReadObjectModuleChangeNotOnMaster()
      throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    GoogleRobotUsernamePassword deserialized =
        (GoogleRobotUsernamePassword) xstream.fromXML(xml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof FakeModule);
    // WithoutJenkins so we are an OtherFakeModule
    assertTrue(deserialized.module instanceof OtherFakeModule);
  }

  private static final String CREDENTIALS_ID = "bazinga";
  private static final String NAME = "Source (foo.com:bar-baz)";
  private static final String USERNAME = "leeroy@google.com";
  private static final String PASSWORD = "Th1s 1s M4 P4SsW0Rd!!!1!";
}