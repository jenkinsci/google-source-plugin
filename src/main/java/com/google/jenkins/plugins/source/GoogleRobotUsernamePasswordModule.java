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
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;

import hudson.util.Secret;

/**
 * Module to abstract the instantiation of dependencies of the
 * {@link GoogleRobotUsernamePassword} plugin.
 */
public class GoogleRobotUsernamePasswordModule
    implements Serializable {

  /**
   * @param strategy the strategy to give username.
   */
  public GoogleRobotUsernamePasswordModule(Strategy strategy) {
    this.strategy = checkNotNull(strategy);
  }

  public Strategy getStrategy() {
    return strategy;
  }
  private final Strategy strategy;

  /**
   * Retrieve the identity associated with the given
   * {@link GoogleRobotCredentials}.
   */
  public String getIdentity(GoogleRobotCredentials credentials) {
    return getStrategy().getUsername(credentials);
  }

  public GoogleOAuth2ScopeRequirement getRequirement() {
    return getStrategy().getScope();
  }

  /**
   * Retrieve an access token for the given {@link GoogleRobotCredentials}.
   */
  public Secret getToken(GoogleRobotCredentials credentials) {
    return credentials.getAccessToken(getRequirement());
  }

  /**
   * Return a version of the module that is capable of being used on
   * a remote machine, with a specific set of credentials.
   */
  public GoogleRobotUsernamePasswordModule forRemote(
      GoogleRobotCredentials credentials) throws GeneralSecurityException {
    return new ForRemote(this, credentials);
  }

  /**
   * For {@link Serializable}
   */
  private static final long serialVersionUID = 42L;

  /**
   * Strategy for choosing the username based on URL patterns.
   */
  public static enum Strategy {
    /* for Gerrit source hosting */
    GERRIT(new Domain("gerrit", "",
        ImmutableList.of(
            new SchemeSpecification("https"),
            new HostnameSpecification("*.googlesource.com", "")
        )), new GerritSourceScopeRequirement()) {
      /** {@inheritDoc} */
      @Override
      public String getUsername(GoogleRobotCredentials credentials) {
        return "git";
      }
    },
    /* for Google Cloud Platform source hosting */
    CLOUD_PLATFORM(new Domain("cloud_platform", "",
        ImmutableList.of(
            new SchemeSpecification("https"),
            new HostnameSpecification(Joiner.on(",").join(
                "code.google.com",
                "source.developers.google.com"), "")
        )), new CloudPlatformSourceScopeRequirement()) {
      /** {@inheritDoc} */
      @Override
      public String getUsername(GoogleRobotCredentials credentials) {
        return credentials.getUsername();
      }
    };

    Strategy(Domain domain, GoogleOAuth2ScopeRequirement scope) {
      this.domain = checkNotNull(domain);
      this.scope = checkNotNull(scope);
    }

    /**
     * @return the domain that this strategy supports.
     */
    public Domain getDomain() {
      return this.domain;
    }
    private final Domain domain;

    /**
     * @return the OAuth scope for this strategy.
     */
    public GoogleOAuth2ScopeRequirement getScope() {
      return this.scope;
    }
    private final GoogleOAuth2ScopeRequirement scope;

    /**
     * @param requirements provided {@link DomainRequirement} to check.
     * @return whether the strategy could be applied to the given requirements.
     */
    public boolean matches(List<DomainRequirement> requirements) {
      return domain.test(requirements);
    }

    /**
     * @param credentials a given {@link GoogleRobotCredentials}.
     * @return the username based on the given {@link GoogleRobotCredentials}.
     */
    public abstract String getUsername(GoogleRobotCredentials credentials);

  }

  /**
   * This module overrides the base module type to provide the
   * given identity outside of the
   */
  private static class ForRemote extends GoogleRobotUsernamePasswordModule {
    public ForRemote(GoogleRobotUsernamePasswordModule parent,
        GoogleRobotCredentials credentials) throws GeneralSecurityException {
      super(parent.getStrategy());
      this.identity = parent.getIdentity(credentials);
      this.credentials = credentials.forRemote(parent.getRequirement());
    }

    /** {@inheritDoc} */
    @Override
    public GoogleRobotUsernamePasswordModule forRemote(
        GoogleRobotCredentials credentials) {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getIdentity(GoogleRobotCredentials credentials) {
      return identity;
    }
    private final String identity;

    /** {@inheritDoc} */
    @Override
    public Secret getToken(GoogleRobotCredentials credentials) {
      return super.getToken(this.credentials);
    }
    private final GoogleRobotCredentials credentials;
  }
}
