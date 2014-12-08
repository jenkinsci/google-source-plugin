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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.metadata.MetadataContainer;

/**
 * Unit test for {@link SourceMetadata}.
 */
public class SourceMetadataTest {

  private SourceMetadata underTest;

  @Before
  public void setup() {
    this.underTest = new SourceMetadata("scm", "repoUrl", "branch", "revision",
        "lastAuthor");
  }

  /**
   * TODO(nghia): Reinstate this test after we fix the "find subtypes" issues.
   */
  // @Test
  // public void jsonRoundTrip() {
  //   // test that serializing a deserialized object recovers the the original.
  //   SourceMetadata sourceInfo = underTest;
  //   SourceMetadata deserialized = serializeThenDeserialize(sourceInfo);
  //   assertEquals(sourceInfo, deserialized);
  //   assertEquals(sourceInfo.toString(), deserialized.toString());
  //   assertEquals(sourceInfo.hashCode(), deserialized.hashCode());
  // }

  private SourceMetadata serializeThenDeserialize(SourceMetadata sourceInfo) {
    MetadataContainer container = new MetadataContainer();
    String serialized = container.listSerialize(ImmutableList.of(sourceInfo));
    System.err.println(serialized);
    return container.deserialize(SourceMetadata.class, serialized);
  }

  @Test
  public void equals() {
    assertFalse(underTest.equals(null));
    assertFalse(underTest.equals(new Object()));
    String scm = underTest.getSCM();
    String repoUrl = underTest.getRepoUrl();
    String branch = underTest.getBranch();
    String revision = underTest.getRevision();
    String lastAuthor = underTest.getLastAuthor();
    assertEquals(underTest, new SourceMetadata(scm, repoUrl, branch, revision,
        lastAuthor));
    assertNotEquals(underTest, new SourceMetadata(scm + "delta", repoUrl,
        branch, revision, lastAuthor));
    assertNotEquals(underTest, new SourceMetadata(scm, repoUrl + "delta",
        branch, revision, lastAuthor));
    assertNotEquals(underTest, new SourceMetadata(scm, repoUrl,
        branch + "delta", revision, lastAuthor));
    assertNotEquals(underTest, new SourceMetadata(scm, repoUrl, branch,
        revision + "delta", lastAuthor));
  }

  @Test
  public void mergeFrom() {
    assertEquals(
        new SourceMetadata("a", "b", "c", "d", "e"),
        new SourceMetadata("a", "b", "c", "d", "e").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("1", "b", "c", "d", "e"),
        new SourceMetadata("", "b", "c", "d", "e").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("a", "2", "c", "d", "e"),
        new SourceMetadata("a", "", "c", "d", "e").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("a", "b", "3", "d", "e"),
        new SourceMetadata("a", "b", "", "d", "e").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("a", "b", "c", "4", "e"),
        new SourceMetadata("a", "b", "c", "", "e").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("a", "b", "c", "d", "5"),
        new SourceMetadata("a", "b", "c", "d", "").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));

    assertEquals(
        new SourceMetadata("1", "2", "3", "4", "5"),
        new SourceMetadata("", "", "", "", "").mergeFrom(
            new SourceMetadata("1", "2", "3", "4", "5")));
  }
}
