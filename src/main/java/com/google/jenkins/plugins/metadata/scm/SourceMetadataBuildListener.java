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

import com.google.jenkins.plugins.metadata.MetadataContainer;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

/**
 * {@code SCMListener} that populates the {@link SourceMetadata} of a build with
 * revision and branch information.
 */
@Extension
public class SourceMetadataBuildListener extends SCMListener {

  /**
   * Seems that {@code SCMListener}s still need to call #register() manually.
   */
  static {
    new SourceMetadataBuildListener().register();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onChangeLogParsed(
      Run<?, ?> build,
      SCM scm,
      TaskListener listener,
      ChangeLogSet<?> changelog) throws Exception {
    for (SourceMetadata source : SourceMetadataExtractor.extract(build, scm,
        changelog)) {
      MetadataContainer.of(build).add(source);
    }
  }
}
