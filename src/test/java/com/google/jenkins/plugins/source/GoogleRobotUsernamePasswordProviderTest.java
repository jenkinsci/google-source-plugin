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

import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import org.acegisecurity.Authentication;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentialsModule;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import jenkins.model.Jenkins;

/**
 * Tests for {@link GoogleRobotUsernamePasswordProvider}.
 */
public class GoogleRobotUsernamePasswordProviderTest {
  @Rule public JenkinsRule jenkins = new JenkinsRule();

  /**
   * Provides a convenient concrete GoogleRobotCredentials class to drive
   * GoogleRobotUsernamePasswordProvider.getCredentials() in tests.
   */
  @NameWith(value = Namer.class, priority = 50)
  private static class FakeGoogleRobotCredentials
      extends GoogleRobotCredentials {
    public FakeGoogleRobotCredentials(String id) {
      super(id, new GoogleRobotCredentialsModule() {
          @Override
          public HttpTransport getHttpTransport() {
            throw new UnsupportedOperationException();
          }

          @Override
          public JsonFactory getJsonFactory() {
            throw new UnsupportedOperationException();
          }
        });
    }

    @Override
    public Credential getGoogleCredential(
        GoogleOAuth2ScopeRequirement requirement)
        throws GeneralSecurityException {
      throw new UnsupportedOperationException();
    }

    @Override
    public CredentialsScope getScope() {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getUsername() {
      throw new UnsupportedOperationException();
    }
  }

  /** Filler class to supplement the Fake. */
  public static class Namer
      extends CredentialsNameProvider<FakeGoogleRobotCredentials> {
    @NonNull
    public String getName(@NonNull FakeGoogleRobotCredentials c) {
      return "TheNameOf" + c.getId();
    }
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    underTest = new GoogleRobotUsernamePasswordProvider(
        new Function<GoogleRobotUsernamePasswordModule.Strategy,
            GoogleRobotUsernamePasswordModule>() {
          @Nullable
          @Override
          public GoogleRobotUsernamePasswordModule apply(
              GoogleRobotUsernamePasswordModule.Strategy strategy) {
            return new FakeModule(strategy);
          }
        });
    // Injecting the fakeProvider in this way is with the following motivation.
    // 1) It makes a reasonable test that our own Provider continues to work if
    // CredentialsProvider undergoes maintenance.
    // 2) It ensures iterating through the 'Extensions' has known ordering
    // between our GoogleRobotCredentials provider and our
    // GoogleRobotUsernamePassword provider so we can reason about ordered
    // execution of mock calls.
    // 3) Trying to make the class an actual {@code @Extension}, if it can even
    // be done with a Mockito Mock, has the side effect that it is visible to
    // all other unittests in this package and gets incorporated by their
    // CredentialProvider calls, breaking them horribly.
    ExtensionList<CredentialsProvider> extensionList =
        Jenkins.getInstance().getExtensionList(CredentialsProvider.class);
    if (!(extensionList.get(extensionList.size() - 1)
        instanceof FakeCredentialsProvider)) {
      extensionList.add(extensionList.size(), fakeProvider);
    }
  }
  GoogleRobotUsernamePasswordProvider underTest;
  @Mock FakeCredentialsProvider fakeProvider;

  private static class FakeModule extends GoogleRobotUsernamePasswordModule {
    public FakeModule(Strategy strategy) {
      super(strategy);
    }

    @Override
    public String getIdentity(GoogleRobotCredentials credentials) {
      throw new UnsupportedOperationException();
    }

    @Override
    public GoogleRobotUsernamePasswordModule forRemote(
        GoogleRobotCredentials credentials) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Because our CredentialsProvider in turn reaches out to CredentialsProvider
   * to obtain GoogleRobotCredentials instances that it can convert to
   * GoogleRobotUsernamePassword, introduce our own fake CredentialsProvider
   * to serve in that capacity and ensure correct end-to-end with the framework.
   */
  public abstract static class FakeCredentialsProvider
      extends CredentialsProvider {
    public FakeCredentialsProvider() {  }

    @Override
    @NonNull
    public <C extends Credentials> List<C> getCredentials(
        @NonNull Class<C> type, ItemGroup itemGroup,
        Authentication authentication) {
      throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public <C extends Credentials> List<C> getCredentials(
        @NonNull Class<C> type, ItemGroup itemGroup,
        Authentication authentication,
        @NonNull List<DomainRequirement> domainRequirements) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Helper function to create a set of GoogleRobotCredentials instances with
   * known ids.  Commonly used to drive mock behavior for the return value of
   * FakeCredentialsProvider's getCredentials() output.
   */
  List<GoogleRobotCredentials> getInputCredentials(List<String> credentialIds) {
    List<GoogleRobotCredentials> list =
        new LinkedList<GoogleRobotCredentials>();
    for (String id : credentialIds) {
      list.add(new FakeGoogleRobotCredentials(id));
    }
    return list;
  }

  /**
   * Helper function to create a set of GoogleRobotUsernamePassword instances
   * with known ids.  Commonly used to drive expected outputs of tested
   * function calls.
   */
  List<? super GoogleRobotUsernamePassword> getExpectedOutputCredentials(
      List<String> credentialIds,
      GoogleRobotUsernamePasswordModule.Strategy strategy) {
    List<GoogleRobotUsernamePassword> list =
        new LinkedList<GoogleRobotUsernamePassword>();
    for (String id : credentialIds) {
      list.add(new GoogleRobotUsernamePassword(id, new FakeModule(strategy)));
    }
    return list;
  }

  /**
   * Matcher used to verify the class type of a form of DomainRequirement.
   * Typed to interact with the Credentials API's expectation of
   * {@code List<DomainRequirement>} rather than, say,
   * {@code Iterable<? extends DomainRequirement>}.
   */
  class TypeMatcher extends BaseMatcher<DomainRequirement> {
    private final Class<? extends DomainRequirement> clazz;

    TypeMatcher(Class<? extends DomainRequirement> clazz) {
      this.clazz = clazz;
    }

    @Override
    public boolean matches(Object item) {
      return item.getClass().equals(clazz);
    }

    @Override
    public void describeTo(Description description) { }
  }

  /**
   * Matcher typed to interact with the Credentials API's expectation of
   * {@code List<DomainRequirement>} rather than, say,
   * {@code Iterable<? extends DomainRequirement>}.
   */
  public static <T> Matcher<List<T>> containsInAnyOrder(
      final Collection<Matcher<T>> expectedMatchers) {
    return new TypeSafeMatcher<List<T>>() {
      @Override
      public void describeTo(Description description) {  }

      @Override
      public boolean matchesSafely(List<T> actualElements) {
        if (actualElements.size() != expectedMatchers.size()) {
          return false;
        }
        List<Matcher<T>> availableMatchers =
            Lists.newArrayList(expectedMatchers);
        for (T element : actualElements) {
          boolean hasMatch = false;
          Iterator<Matcher<T>> matchersIterator =
              availableMatchers.iterator();
          while (matchersIterator.hasNext()) {
            if (matchersIterator.next().matches(element)) {
              matchersIterator.remove();
              hasMatch = true;
              break;
            }
          }
          if (!hasMatch) {
            return false;
          }
        }
        return true;
      }
    };
  }

  /**
   * Convenience function that takes a list of DomainRequirement on construction
   * and verifies for its match that the input has the same composition of
   * subclasses.  Workaround for DomainRequirement's equals method only
   * performing instance comparison.
   */
  List<DomainRequirement> buildMyDomainRequirementMatcher(
      List<DomainRequirement> requirements) {
    List<Matcher<DomainRequirement>> matchers = Lists.newArrayList();
    for (DomainRequirement req : requirements) {
      matchers.add(new TypeMatcher(req.getClass()));
    }

    Matcher<List<DomainRequirement>> internalMatcher =
        containsInAnyOrder(matchers);

    return argThat(internalMatcher);
  }

  @Test
  public void testImplementationCorrectnessPrerequisite() throws Exception {
    // If these classes can ever be confused as one another, then the risk of
    // infinite recursion providing credentials is greatly increased as we
    // make a CredentialsProvider.lookupCredentials() call in fulfillment of
    // getCredentials() which is in turn called by lookupCredentials() and
    // rely on fast stopping by the requested credential type to avoid epic
    // fail.
    assertFalse(GoogleRobotUsernamePassword.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    assertFalse(GoogleRobotCredentials.class.isAssignableFrom(
        GoogleRobotUsernamePassword.class));
  }

  @Test
  public void testGetCredentialsNoResultsOnWrongCredentialType()
      throws Exception {
    // Enable the GoogleRobotCredentials to return some fake credentials, but it
    // won't actually happen.
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement()))))
        .thenReturn(getInputCredentials(ImmutableList.of("foo")));

    assertEquals(new LinkedList<GoogleRobotCredentials>(),
        underTest.getCredentials(GoogleRobotCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM,
            ImmutableList.<DomainRequirement>of()));
    // The above path should not only return no credentials, but also fail fast
    // to avoid any lookup of prospective source credentials.
    verifyZeroInteractions(fakeProvider);
  }

  @Test
  public void testEndToEndNoRequirements_CloudPlatform() throws Exception {
    // Verify a CredentialProvider lookup call for a relatively deeply nested
    // class that is still satisfiable by GoogleRobotUsernamePassword.
    List<DomainRequirement> requirements = Lists.newLinkedList();
    List<String> expectedCredentialsIds = Lists.newArrayList("foo", "bar");
    // Quickly sanity-check this behavior or else the mocked return values
    // proscribed may not reflect reality and we risk infite recursion inside
    // the implementation class.
    assertFalse(UsernamePasswordCredentials.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    // Proscribe mock behavior.
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement()))))
        .thenReturn(getInputCredentials(expectedCredentialsIds));
    when(fakeProvider.getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            requirements))).thenReturn(
        new LinkedList<UsernamePasswordCredentials>());

    assertEquals(getExpectedOutputCredentials(expectedCredentialsIds,
            GoogleRobotUsernamePasswordModule.Strategy.CLOUD_PLATFORM),
        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, requirements));

    verify(fakeProvider).getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement())));
    verify(fakeProvider).getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(requirements)
    );
  }

  @Test
  public void testEndToEndNoRequirements_Gerrit() throws Exception {
    // Verify a CredentialProvider lookup call for a relatively deeply nested
    // class that is still satisfiable by GoogleRobotUsernamePassword.
    List<DomainRequirement> requirements = Lists.newLinkedList();
    List<String> expectedCredentialsIds = Lists.newArrayList("foo", "bar");
    // Quickly sanity-check this behavior or else the mocked return values
    // proscribed may not reflect reality and we risk infite recursion inside
    // the implementation class.
    assertFalse(UsernamePasswordCredentials.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    // Proscribe mock behavior.
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new GerritSourceScopeRequirement()))))
        .thenReturn(getInputCredentials(expectedCredentialsIds));
    when(fakeProvider.getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            requirements))).thenReturn(
        new LinkedList<UsernamePasswordCredentials>());

    assertEquals(getExpectedOutputCredentials(expectedCredentialsIds,
            GoogleRobotUsernamePasswordModule.Strategy.GERRIT),
        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, requirements));

    verify(fakeProvider).getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement())));
    verify(fakeProvider).getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(requirements)
    );
  }

  @Test
  public void testEndToEnd_Gerrit() throws Exception {
    // This is exactly like testEndToEndNoRequirements_CloudPlatform, except
    // it adds a DomainRequirement that is consistent with the credential type.
    List<DomainRequirement> requirements =
        Lists.<DomainRequirement>newArrayList(
            new HostnameRequirement("jenkins.googlesource.com"),
            new SchemeRequirement("https")
        );
    List<String> expectedCredentialsIds = Lists.newArrayList("foo", "bar");
    // Quickly sanity-check this behavior or else the mocked return values
    // proscribed may not reflect reality and we risk infite recursion inside
    // the implementation class.
    assertFalse(UsernamePasswordCredentials.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    // Proscribe mock behavior.
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new GerritSourceScopeRequirement()))))
        .thenReturn(getInputCredentials(expectedCredentialsIds));
    when(fakeProvider.getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            requirements))).thenReturn(
        new LinkedList<UsernamePasswordCredentials>());

    assertEquals(getExpectedOutputCredentials(expectedCredentialsIds,
            GoogleRobotUsernamePasswordModule.Strategy.GERRIT),
        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, requirements));

    verify(fakeProvider).getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new GerritSourceScopeRequirement())));
    verify(fakeProvider).getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(requirements));
    verifyNoMoreInteractions(fakeProvider);
  }

  @Test
  public void testEndToEnd_Mismatch() throws Exception {
    // Request for googlesource.com but only credentials with cloud platform
    // scope is provided.
    List<DomainRequirement> requirements =
        Lists.<DomainRequirement>newArrayList(
            new HostnameRequirement("jenkins.googlesource.com"),
            new SchemeRequirement("https")
        );
    List<String> expectedCredentialsIds = Lists.newArrayList("foo", "bar");
    // Quickly sanity-check this behavior or else the mocked return values
    // proscribed may not reflect reality and we risk infite recursion inside
    // the implementation class.
    assertFalse(UsernamePasswordCredentials.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    // Proscribe mock behavior, but provides only CloudPlatform scopexs
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement())
        )
    ))
        .thenReturn(getInputCredentials(expectedCredentialsIds));
    when(fakeProvider.getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            requirements))).thenReturn(
        new LinkedList<UsernamePasswordCredentials>());

    // no matching credential
    assertEquals(Lists.newArrayList(),
        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, requirements));

    verify(fakeProvider).getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new GerritSourceScopeRequirement())));
    verify(fakeProvider).getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(requirements));
    verifyNoMoreInteractions(fakeProvider);
  }

  @Test
  public void testEndToEndWithUnsatisfiedRequirements() throws Exception {
    // This is exactly like testEndToEnd_CloudFlatform, except now
    // the DomainRequirement used is incompatible, so our provider will fast-
    // quit and neither return credentials nor invoke the mock itself (the mock
    // still gets the one invocation direct from the CredentialProvider).
    List<DomainRequirement> requirements =
        Lists.<DomainRequirement>newArrayList(
            new SchemeRequirement("ssh"));
    List<String> expectedCredentialsIds = Lists.newArrayList("foo", "bar");
    // Quickly sanity-check this behavior or else the mocked return values
    // proscribed may not reflect reality and we risk infite recursion inside
    // the implementation class.
    assertFalse(UsernamePasswordCredentials.class.isAssignableFrom(
        GoogleRobotCredentials.class));
    // Proscribe mock behavior.
    when(fakeProvider.getCredentials(
        eq(GoogleRobotCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            ImmutableList.<DomainRequirement>of(
                new CloudPlatformSourceScopeRequirement()))))
        .thenReturn(getInputCredentials(expectedCredentialsIds));
    when(fakeProvider.getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(
            requirements))).thenReturn(
                new LinkedList<UsernamePasswordCredentials>());

    assertEquals(new LinkedList<UsernamePasswordCredentials>(),
        CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class,
            Jenkins.getInstance(), ACL.SYSTEM, requirements));

    verify(fakeProvider).getCredentials(
        eq(UsernamePasswordCredentials.class), eq(Jenkins.getInstance()),
        eq(ACL.SYSTEM), buildMyDomainRequirementMatcher(requirements));
    verifyNoMoreInteractions(fakeProvider);
  }
}
