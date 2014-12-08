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

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM.EntryImpl;
import org.jvnet.hudson.test.FakeChangeLogSCM.FakeChangeLogSet;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.jenkins.plugins.metadata.MetadataContainer;
import com.google.jenkins.plugins.metadata.MetadataValue;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.mercurial.MercurialSCM;
import hudson.plugins.mercurial.MercurialTagAction;
import hudson.scm.SCM;

/**
 * Unit test for {@link SourceMetadataBuildListener}.
 */
public class SourceMetadataBuildListenerTest {

  private static final String REVISION =
      "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
  private static final String REPO_URL =
      "https://code.google.com/a/example.com/p/foo";
  private static final String AUTHOR_1 =
      "First Test Author Name";
  private static final String AUTHOR_2 =
      "Second Test Author Name";

  @Rule public JenkinsRule jenkins = new JenkinsRule();

  private SourceMetadataBuildListener underTest;

  private FreeStyleProject project;
  private AbstractBuild<?, ?> build;
  private int buildNum;
  private FakeChangeLogSet changes;

  @Mock private BuildData buildData;
  @Mock private MercurialSCM mercurialScm;
  @Mock private MultiSCM multiScm;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    underTest = new SourceMetadataBuildListener();
    project = jenkins.createFreeStyleProject();
    build = new FreeStyleBuild(project);
    buildNum = build.getNumber();
    ArrayList<EntryImpl> changeList = new ArrayList<EntryImpl>();
    changeList.add(new EntryImpl().withAuthor(AUTHOR_1));
    changeList.add(new EntryImpl().withAuthor(AUTHOR_2));
    changes = new FakeChangeLogSet(build, changeList);
  }

  @Test
  public void onChangeLogParsed_noInfo() throws Exception {
    assertTrue(MetadataContainer.getMetadata(build).isEmpty());

    underTest.onChangeLogParsed(build, null, null, new FakeChangeLogSet(build,
        new ArrayList<EntryImpl>()));

    assertTrue(MetadataContainer.getMetadata(build).isEmpty());
  }

  @Test
  public void onChangeLogParsed_git() throws Exception {
    build.addAction(buildData);
    Revision revision = new Revision(ObjectId.fromString(REVISION));
    when(buildData.getRemoteUrls()).thenReturn(ImmutableSet.of(REPO_URL));
    when(buildData.getLastBuiltRevision()).thenReturn(revision);
    when(buildData.getBuildsByBranchName()).thenReturn(ImmutableMap.of(
        "wrong-branch", new Build(revision, buildNum + 1, Result.SUCCESS),
        "right-branch", new Build(revision, buildNum, Result.SUCCESS)));

    underTest.onChangeLogParsed(build, null, null, changes);

    SourceMetadata source = (SourceMetadata) Iterables.getOnlyElement(
        MetadataContainer.getMetadata(build).get(SourceMetadata.SOURCE_KEY));
    assertNotNull(source);
    assertEquals("git", source.getSCM());
    assertEquals("right-branch", source.getBranch());
    assertEquals(REVISION, source.getRevision());
    assertEquals(REPO_URL, source.getRepoUrl());
    assertEquals(AUTHOR_1, source.getLastAuthor());
  }

  @Test
  public void onChangeLogParsed_mercurial() throws Exception {
    project.setScm(mercurialScm);
    when(mercurialScm.getBranch()).thenReturn("branchname");
    when(mercurialScm.getSource()).thenReturn(REPO_URL);
    when(mercurialScm.getSubdir()).thenReturn("foo");
    build.addAction(new MercurialTagAction(REVISION, "7", "foo"));

    underTest.onChangeLogParsed(build, mercurialScm, null, changes);

    SourceMetadata source = (SourceMetadata) Iterables.getOnlyElement(
        MetadataContainer.getMetadata(build).get(SourceMetadata.SOURCE_KEY));
    assertNotNull(source);
    assertEquals("mercurial", source.getSCM());
    assertEquals("branchname", source.getBranch());
    assertEquals(REVISION, source.getRevision());
    assertEquals(REPO_URL, source.getRepoUrl());
    assertEquals(AUTHOR_1, source.getLastAuthor());
  }

  @Test
  public void onChangeLogParsed_multiSCM() throws Exception {
    project.setScm(multiScm);
    when(multiScm.getConfiguredSCMs())
        .thenReturn(ImmutableList.<SCM>of(mercurialScm));
    when(mercurialScm.getBranch()).thenReturn("hg-branch");
    when(mercurialScm.getSource()).thenReturn(REPO_URL);
    when(mercurialScm.getSubdir()).thenReturn("foo");
    build.addAction(new MercurialTagAction(REVISION, "7", "foo"));
    build.addAction(buildData);
    Revision revision = new Revision(ObjectId.fromString(REVISION));
    when(buildData.getRemoteUrls()).thenReturn(ImmutableSet.of(REPO_URL));
    when(buildData.getLastBuiltRevision()).thenReturn(revision);
    when(buildData.getBuildsByBranchName()).thenReturn(ImmutableMap.of(
        "git-branch", new Build(revision, buildNum, Result.SUCCESS)));

    underTest.onChangeLogParsed(build, multiScm, null, changes);

    ImmutableList<MetadataValue> metadata = ImmutableList.copyOf(
        MetadataContainer.getMetadata(build).get(SourceMetadata.SOURCE_KEY));
    assertEquals(2, metadata.size());
    assertTrue(metadata.contains(new SourceMetadata(
        "git", REPO_URL, "git-branch", REVISION, AUTHOR_1)));
    assertTrue(metadata.contains(new SourceMetadata(
        "mercurial", REPO_URL, "hg-branch", REVISION, AUTHOR_1)));
  }
}
