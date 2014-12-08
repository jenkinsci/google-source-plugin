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
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import jenkins.model.Jenkins;

/**
 * Utility class to handle common operations with Jenkins#getInstance() to avoid
 * a lot of boiler-plate or unsafe operations.
 */
public class JenkinsUtils {

  /**
   * Similar to Jenkins#getExtensionList() but return empty collection in case
   * running without Jenkins.
   */
  public static <T extends ExtensionPoint> List<T>
            getExtensionList(Class<T> klass) {
    checkNotNull(klass);

    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      return ImmutableList.<T> of();
    } else {
      return Objects.firstNonNull(jenkins.getExtensionList(klass),
          ImmutableList.<T> of());
    }
  }

  /**
   * Similar to Jenkins#getDescriptorList() but return empty collection in case
   * running without Jenkins.
   */
  public static <T extends Describable<T>> List<Descriptor<T>>
            getDescriptorList(Class<T> klass) {
    checkNotNull(klass);

    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      return ImmutableList.<Descriptor<T>> of();
    } else {
      return Objects.firstNonNull(jenkins.getDescriptorList(klass),
          ImmutableList.<Descriptor<T>> of());
    }
  }

  /**
   * @return whether is plugin is installed.
   */
  public static boolean isPluginInstalled(String pluginName) {
    checkNotNull(pluginName);

    Jenkins jenkins = Jenkins.getInstance();
    return (jenkins != null) && (jenkins.getPlugin(pluginName) != null);
  }


  /**
   * @return a set of {@link Label} used in this Jenkins, or empty set if
   *         {@link Jenkins#getInstance()} is null.
   */
  public static Collection<Label> getLabels() {
    Jenkins jenkins = Jenkins.getInstance();
    return jenkins == null ? ImmutableList.<Label> of() : jenkins.getLabels();
  }

  /**
   * @return {@link Jenkins#getInstance()} if that isn't null, or die.
   */
  public static Jenkins getInstanceOrDie() {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins == null) {
      throw new IllegalStateException("Jenkins is not running");
    }
    return jenkins;
  }

}
