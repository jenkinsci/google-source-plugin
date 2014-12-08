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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.jenkinsci.plugins.multiplescms.MultiSCM;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.mercurial.MercurialSCM;
import hudson.plugins.mercurial.MercurialTagAction;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

/**
 * Provides extension points to extract {@link SourceMetadata} from various
 * aspects of a build.
 */
public class SourceMetadataExtractor {

  /**
   * Module wrapping static methods for mocking.
   */
  public interface Module {
    Iterable<SourceMetadata> extract(Run<?, ?> build, SCM scm,
        ChangeLogSet<?> changelog);
    Multimap<String, SourceMetadata> extractFromBuildActions(
        Run<?, ?> build, ChangeLogSet<?> changelog);
    Multimap<String, SourceMetadata> extractFromSCM(SCM scm);
  }

  private static final Module DEFAULT_MODULE = new Module() {
      @Override
      public Iterable<SourceMetadata> extract(Run<?, ?> build, SCM scm,
          ChangeLogSet<?> changelog) {
        return SourceMetadataExtractor.extract(build, scm, changelog);
      }

      @Override
      public Multimap<String, SourceMetadata> extractFromBuildActions(
          Run<?, ?> build, ChangeLogSet<?> changelog) {
        return FromBuildActions.extract(build, changelog);
      }

      @Override
      public Multimap<String, SourceMetadata> extractFromSCM(SCM scm) {
        return FromSCM.extract(scm);
      }
    };

  /**
   * Returns a {@code Module} that calls the default
   * {@code SourceMetadataExtractor} implementation.
   */
  public static Module getDefaultModule() {
    return DEFAULT_MODULE;
  }

  /**
   * Extracts zero or more {@code SourceMetadata} from the given build and
   * returns them.
   *
   * <p>Currently there are extension points to extract metadata from build
   * actions and the project's SCM.
   */
  @Nullable
  public static Iterable<SourceMetadata> extract(Run<?, ?> build, SCM scm,
      ChangeLogSet<?> changelog) {
    HashMap<String, SourceMetadata> merged = Maps.newHashMap();
    List<SourceMetadata> notDeduped = Lists.newArrayList();
    for (Map.Entry<String, SourceMetadata> entry : Iterables.concat(
        FromBuildActions.extract(build, changelog).entries(),
        FromSCM.extract(scm).entries())) {
      if (entry.getKey().isEmpty()) {
        // The empty string is a special key that means "don't dedupe".
        notDeduped.add(entry.getValue());
      } else {
        // For nonempty keys, look for a previously processed SourceMetadata and
        // merge with it if it exists.
        SourceMetadata metadata = merged.get(entry.getKey());
        if (metadata != null) {
          metadata = metadata.mergeFrom(entry.getValue());
        } else {
          metadata = entry.getValue();
        }
        merged.put(entry.getKey(), metadata);
      }
    }
    return Iterables.concat(merged.values(), notDeduped);
  }

  /**
   * Extension point for extracting {@link SourceMetadata} from build actions.
   */
  public abstract static class FromBuildActions implements ExtensionPoint {

    /**
     * Runs all registered extensions to extract metadata from the given
     * {@code Run} and {@code ChangeLogSet} and returns them.
     */
    public static Multimap<String, SourceMetadata> extract(
        Run<?, ?> build, ChangeLogSet<?> changelog) {
      ImmutableListMultimap.Builder<String, SourceMetadata> results =
          ImmutableListMultimap.builder();
      if (build == null) {
        return results.build();
      }
      for (FromBuildActions extractor : all()) {
        results.putAll(extractor.getSourceMetadata(build, changelog));
      }
      return results.build();
    }

    protected static String getLatestAuthor(ChangeLogSet<?> changeSet) {
      if (changeSet.isEmptySet()) {
        return "";
      }
      // ChangeLogSet specifies that the iterator returns newest entries first.
      return changeSet.iterator().next().getAuthor().getId();
    }

    private static Collection<FromBuildActions> all() {
      return JenkinsUtils.getExtensionList(FromBuildActions.class);
    }

    /**
     * Extracts and returns metadata from a {@code Run} and associated
     * {@code ChangeLogSet}. After all extensions run, metadata with the same
     * map key are merged to allow multiple extensions to contribute to a
     * single repo's metadata.
     */
    protected abstract Multimap<String, SourceMetadata> getSourceMetadata(
        Run<?, ?> build, ChangeLogSet<?> changelog);
  }

  /**
   * Extension point for extracting {@link SourceMetadata} from specific
   * {@code SCM} subclasses.
   */
  public abstract static class FromSCM implements ExtensionPoint {

    /**
     * Runs applicable registered extensions to extract metadata from the given
     * {@code SCM} and returns them.
     */
    public static Multimap<String, SourceMetadata> extract(SCM scm) {
      ImmutableListMultimap.Builder<String, SourceMetadata> results =
          ImmutableListMultimap.builder();
      if (scm == null) {
        return results.build();
      }
      for (FromSCM extractor : all()) {
        if (extractor.isApplicable(scm.getClass())) {
          results.putAll(extractor.getSourceMetadata(scm));
        }
      }
      return results.build();
    }

    private static Collection<FromSCM> all() {
      return JenkinsUtils.getExtensionList(FromSCM.class);
    }

    private final Class<?> supportedClass;

    public FromSCM(Class<?> supportedClass) {
      this.supportedClass = checkNotNull(supportedClass);
    }

    protected boolean isApplicable(Class<?> scmClass) {
      return supportedClass.isAssignableFrom(scmClass);
    }

    /**
     * Extracts and returns metadata from an instance of {@code T}. After all
     * extensions run, metadata with the same map key are merged to allow
     * multiple extensions to contribute to a single repo's metadata.
     *
     * @param scm SCM instance to extract metadata from; will be assignable to
     *            the {@code supportedClass} passed to the {@code FromSCM}
     *            constructor.
     */
    protected abstract Multimap<String, SourceMetadata> getSourceMetadata(
        SCM scm);
  }

  /**
   * {@code SourceMetadataExtractor.FromBuildActions} for Git.
   */
  public static class FromGitBuildActions extends FromBuildActions {

    @Extension
    public static FromGitBuildActions newInstance() {
      return JenkinsUtils.isPluginInstalled("git")
          ? new FromGitBuildActions()
          : null;
    }

    @Override
    protected Multimap<String, SourceMetadata> getSourceMetadata(
        Run<?, ?> build, ChangeLogSet<?> changelog) {
      ImmutableListMultimap.Builder<String, SourceMetadata> metadata =
          ImmutableListMultimap.builder();
      int counter = 0;
      String commitAuthor = getLatestAuthor(changelog);
      for (BuildData buildData : build.getActions(BuildData.class)) {
        String revision = buildData.getLastBuiltRevision().getSha1String();
        String branch = getBranchForBuild(build, buildData);

        for (String url : buildData.getRemoteUrls()) {
          // The empty string is a special key that means "don't dedupe".
          metadata.put("", new SourceMetadata("git", url, branch, revision,
              commitAuthor));
        }
      }
      return metadata.build();
    }

    @Nullable
    private String getBranchForBuild(
        Run<?, ?> build, BuildData buildData) {
      checkNotNull(buildData);
      String branch = null;
      for (Map.Entry<String, Build> entry
          : buildData.getBuildsByBranchName().entrySet()) {
        if (build.getNumber() == entry.getValue().getBuildNumber()) {
          branch = entry.getKey();
        }
      }
      return branch;
    }
  }

  /**
   * {@code SourceMetadataExtractor.FromBuildActions} for Mercurial.
   */
  public static class FromMercurialBuildActions extends FromBuildActions {

    @Extension
    public static FromMercurialBuildActions newInstance() {
      return JenkinsUtils.isPluginInstalled("mercurial")
          ? new FromMercurialBuildActions()
          : null;
    }

    @Override
    protected Multimap<String, SourceMetadata> getSourceMetadata(
        Run<?, ?> build, ChangeLogSet<?> changelog) {
      ImmutableListMultimap.Builder<String, SourceMetadata> metadata =
          ImmutableListMultimap.builder();
      String commitAuthor = getLatestAuthor(changelog);
      for (MercurialTagAction tagAction
          : build.getActions(MercurialTagAction.class)) {
        metadata.put(
            FromMercurialSCM.getDedupeTag(tagAction.getSubdir()),
            new SourceMetadata("mercurial", "", "", tagAction.getId(),
                commitAuthor));
      }
      return metadata.build();
    }
  }

  /**
   * {@code SourceMetadataExtractor.FromSCM} for {@code MercurialSCM}.
   */
  public static class FromMercurialSCM extends FromSCM {

    @Extension
    public static FromMercurialSCM newInstance() {
      return JenkinsUtils.isPluginInstalled("mercurial")
          ? new FromMercurialSCM()
          : null;
    }

    public FromMercurialSCM() {
      super(MercurialSCM.class);
    }

    @Override
    protected Multimap<String, SourceMetadata> getSourceMetadata(SCM scm) {
      MercurialSCM hgScm = (MercurialSCM) scm;
      return ImmutableListMultimap.of(
          getDedupeTag(hgScm.getSubdir()),
          new SourceMetadata(
              "mercurial", hgScm.getSource(), hgScm.getBranch(), "", ""));
    }

    public static String getDedupeTag(String subdir) {
      return "mercurial:" + subdir;
    }
  }

  /**
   * {@code SourceMetadataExtractor.FromSCM} for {@code MultiSCM}.
   */
  public static class FromMultiSCM extends FromSCM {

    @Extension
    public static FromMultiSCM newInstance() {
      return JenkinsUtils.isPluginInstalled("multiple-scms")
          ? new FromMultiSCM()
          : null;
    }

    public FromMultiSCM() {
      super(MultiSCM.class);
    }

    @Override
    protected Multimap<String, SourceMetadata> getSourceMetadata(SCM scm) {
      MultiSCM multiScm = (MultiSCM) scm;
      ImmutableListMultimap.Builder<String, SourceMetadata> metadata =
          ImmutableListMultimap.builder();
      for (SCM innerSCM : multiScm.getConfiguredSCMs()) {
        metadata.putAll(FromSCM.extract(innerSCM));
      }
      return metadata.build();
    }
  }
}
