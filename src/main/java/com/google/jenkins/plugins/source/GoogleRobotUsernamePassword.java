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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;

import static com.google.common.base.Preconditions.checkNotNull;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainRestrictedCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.domains.DomainRequirementProvider;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.util.Secret;


/**
 * This new kind of credential provides an embedded
 * {@link GoogleRobotCredentials} as a username and password for use with a
 * {@link hudson.scm.SCM}.
 * <p>
 * BACKGROUND: For Cloud projects with a private repository, Google accepts a
 * username/password combination that is truly email/oauth-refresh-token.
 * For service accounts, for which a refresh token may not be readily available
 * (e.g. with Compute Engine's metadata service) it also supports using:
 * email/oauth-access-token.
 * <p>
 * This credential wraps a service account credential to provide it in this
 * manner as a {@code StandardUsernamePasswordCredentials} for usage with the
 * new {@code Credentials}-aware {@link hudson.scm.SCM} plugins.
 */
@NameWith(value = GoogleRobotUsernamePassword.NameProvider.class, priority = 99)
public class GoogleRobotUsernamePassword extends BaseStandardCredentials
    implements DomainRestrictedCredentials,
    StandardUsernamePasswordCredentials {
  /**
   * Constructs a username and password credential wrapper around the existing
   * credential, specified by {@code credentialsId}.
   */
  @DataBoundConstructor
  public GoogleRobotUsernamePassword(String credentialsId,
      @Nullable GoogleRobotUsernamePasswordModule module) {
    super(CredentialsScope.GLOBAL, "source:" + credentialsId,
        "doesn't matter" /* description */);
    this.credentialsId = checkNotNull(credentialsId);
    if (module != null) {
      this.module = module;
    } else {
      /* for backward compatiblity */
      this.module = new GoogleRobotUsernamePasswordModule(
          GoogleRobotUsernamePasswordModule.Strategy.CLOUD_PLATFORM);
    }
  }

  /**
   * We do not have a descriptor, so that we won't show up in the user interface
   * as a credential that can be explicitly created, so we will not be
   * discovered by the DescribableDomainRequirementProvider.  Instead, implement
   * our own trivial provider.
   */
  @Extension
  public static class EnclosingDomainRequirementProvider
      extends DomainRequirementProvider {
    /** {@inheritDoc} */
    @Override
    protected <T extends DomainRequirement> List<T> provide(Class<T> type) {
      @Nullable T requirement = of(GoogleRobotUsernamePassword.class, type);
      return (requirement == null) ? ImmutableList.<T>of()
          : ImmutableList.of(requirement);
    }
  }

  /**
   * This type of credentials only works for authenticating against
   * "code.google.com" and "source.developers.google.com" hosted repositories,
   * for which we require "https".
   */
  @Override
  public boolean matches(List<DomainRequirement> requirements) {
    return module.getStrategy().matches(requirements);
  }

  /**
   * Return the unique ID of the inner {@link GoogleRobotCredentials} that this
   * Username/Password proxy is wrapping.
   */
  public String getCredentialsId() {
    return credentialsId;
  }
  private final String credentialsId;

  /**
   * Retrieve our wrapped credentials based on the above ID we store.
   */
  @Nullable
  public GoogleRobotCredentials getCredentials() {
    if (!areOnMaster()) {
      return null;
    }
    return GoogleRobotCredentials.getById(getCredentialsId());
  }

  /**
   * The module used for providing dependencies.
   *
   * NOTE: This would be final, although it is assigned by the deserialization
   * routine {@code readObject()}.
   */
  @VisibleForTesting /*final*/ GoogleRobotUsernamePasswordModule module;

  /**
   * Detect whether we are on the master, to determine how to
   * serialize things.
   */
  private boolean areOnMaster() {
    return Hudson.getInstance() != null;
  }

  /**
   * Support writing our credential to the wire or disk.
   *
   * NOTE: If we aren't on the master, we are expected to provide the embedded
   * credential, along with a module the recipient can use to establish identity
   *
   * It is also worth noting that this serializes superfluous information when
   * we are writing to disk.
   */
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    // Pass a remotable version of our module, tailored to this local
    // credential, to the receiving readObject method.
    // NOTE: If this is simply serializing to disk, the readObject will ignore
    // this when reading it back in.
    try {
      oos.writeObject(module.forRemote(getCredentials()));
    } catch (GeneralSecurityException e) {
      throw new IOException(e);
    }
  }

  /**
   * Support reading our credential from the wire or disk.
   *
   * NOTE: If we aren't on the master, we expect the embedded credential
   * to be provided, along with a module we can use to establish identity.
   */
  private void readObject(ObjectInputStream ois)
      throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    if (!areOnMaster()) {
      // Read in the remotable module, which we will use for things like
      // retrieving identity and credentials.
      this.module = (GoogleRobotUsernamePasswordModule) ois.readObject();
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getDescription() {
    final GoogleRobotCredentials credentials = getCredentials();
    if (credentials == null) {
      return "";
    } else {
      return CredentialsNameProvider.name(credentials);
    }
  }

  /** {@inheritDoc} */
  @Override
  public CredentialsDescriptor getDescriptor() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public String getUsername() {
    return module.getIdentity(getCredentials());
  }

  /** {@inheritDoc} */
  @Override
  public Secret getPassword() {
    return module.getToken(getCredentials());
  }

  /**
   * Provide a name that the user will understand, in the dropdown
   * shown by {@link hudson.scm.SCM}s.
   */
  public static class NameProvider
      extends CredentialsNameProvider<GoogleRobotUsernamePassword> {
    /** {@inheritDoc} */
    @Override
    public String getName(GoogleRobotUsernamePassword c) {
      return Messages.GoogleRobotUsernamePassword_ListingWrapper(
          c.getDescription());
    }
  }
}
