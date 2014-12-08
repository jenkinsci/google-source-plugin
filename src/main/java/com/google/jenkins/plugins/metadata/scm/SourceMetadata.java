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
package com.google.jenkins.plugins.metadata.scm;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.google.common.base.Objects;
import com.google.jenkins.plugins.metadata.MetadataValue;

/**
 * Containing information regarding the source code of a given build. This
 * consists of the SCM being used, the source URL, the branch and the revision
 * being built.
 */
@JsonTypeInfo(use = Id.NAME, include = As.WRAPPER_OBJECT)
@JsonTypeName(value = ".scm.SourceMetadata")
// TODO(nghia): Fix the "register subtype" issue.
public class SourceMetadata implements MetadataValue {
  private final String scm;
  private final String repoUrl;
  private final String branch;
  private final String revision;
  private final String lastAuthor;
  public static final String SOURCE_KEY = "com.google.jenkins.source";

  /**
   * @param scm the SCM being used.
   * @param repoUrl the URL for the repository.
   * @param branch the branch being used.
   * @param revision the code revision being used.
   */
  @JsonCreator
  public SourceMetadata(@JsonProperty("scm") String scm,
      @JsonProperty("repoUrl") String repoUrl,
      @JsonProperty("branch") String branch,
      @JsonProperty("revision") String revision,
      @JsonProperty("lastAuthor") String lastAuthor) {
    this.scm = checkNotNull(scm);
    this.repoUrl = checkNotNull(repoUrl);
    this.branch = checkNotNull(branch);
    this.revision = checkNotNull(revision);
    this.lastAuthor = checkNotNull(lastAuthor);
  }

  /**
   * @return the SCM being used. For examples "git" for git, and "hg" for
   *         Mercurial.
   */
  public String getSCM() {
    return scm;
  }

  /**
   * @return the URL of the repository.
   */
  public String getRepoUrl() {
    return repoUrl;
  }

  /**
   * @return the branch being used.
   */
  public String getBranch() {
    return branch;
  }

  /**
   * @return the revision being used.
   */
  public String getRevision() {
    return revision;
  }

  /**
   * @return the author of the latest recorded change.
   */
  public String getLastAuthor() {
    return lastAuthor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SourceMetadata)) {
      return false;
    }
    SourceMetadata other = (SourceMetadata) obj;
    return equal(scm, other.scm)
        && equal(repoUrl, other.repoUrl)
        && equal(branch, other.branch)
        && equal(revision, other.revision)
        && equal(lastAuthor, other.lastAuthor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(scm, repoUrl, branch, revision, lastAuthor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Objects.toStringHelper(getClass())
        .add("scm", scm)
        .add("repoUrl", repoUrl)
        .add("branch", branch)
        .add("revision", revision)
        .add("lastAuthor", lastAuthor)
        .toString();
  }

  @JsonIgnore
  @Override
  public String getKey() {
    return SOURCE_KEY;
  }

  /**
   * Returns a copy of this instance with empty fields filled in from the given
   * instance.
   */
  public SourceMetadata mergeFrom(SourceMetadata other) {
    return new SourceMetadata(
        (!scm.isEmpty()) ? scm : other.scm,
        (!repoUrl.isEmpty()) ? repoUrl : other.repoUrl,
        (!branch.isEmpty()) ? branch : other.branch,
        (!revision.isEmpty()) ? revision : other.revision,
        (!lastAuthor.isEmpty()) ? lastAuthor : other.lastAuthor);
  }
}
