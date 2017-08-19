Jenkins Google Source Plugin
====================

Read more: http://wiki.jenkins-ci.org/display/JENKINS/Google+Source+Plugin


This plugin provides the credential provider to use Google Cloud Platform OAuth Credentials (provided by the Google OAuth Plugin) to access source code from https://source.developer.google.com as well as https://*.googlesource.com. It supports both kinds of credentials provided by Google OAuth Plugin: Google Service Account from metadata as well as Google Service Account from private key.


1. In order to use a Git repository hosted on https://source.developer.google.com, your credential will need the scope https://www.googleapis.com/auth/source.read_write.  Your service account will need to have access to the repository’s Google Cloud Platform project.

1. In order to use https://*.googlesource.com, your credential will need the scope https://www.googleapis.com/auth/gerritcodereview.  Your service account will need to be whitelisted by the maintainers of that repository.

Usage
===
First, configure your OAuth credentials per instructions from Google OAuth Plugin.

Then, when configuring the Git repository for your Jenkins job, if you enter a https://*.googlesource.com address in the “Repository URL” text area, your Credentials drop box will automatically be populated with credentials having the https://www.googleapis.com/auth/gerritcodereview scope

Similarly, if you enter a https://source.developer.google.com Git repository, your Credentials box will be populated with credentials having the https://www.googleapis.com/auth/source.read_write scope.

Select the required credential, then your job is ready to go!

Development
===========

How to build
--------------

	mvn clean verify

Creates the plugin HPI package for use with Jenkins.


License
-------

	(The Apache v2 License)

    Copyright 2013 Google Inc. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
