#!/bin/sh
#
# Hermes - GWT Server-side I18N Library
# Copyright (C) 2011-2026  Matt Bertolini
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
#
# Publish Hermes to Maven Central:
#
#   scripts/publish.sh snapshot   publish the current -SNAPSHOT version
#   scripts/publish.sh release    upload the current release version, then
#                                 publish it from central.sonatype.com
#
# Secrets come only from these environment variables, which the caller sets:
#
#   CENTRAL_USERNAME, CENTRAL_PASSWORD  Central Portal token, read by
#                                       .mvn/central-settings.xml
#   MAVEN_GPG_KEY, MAVEN_GPG_PASSPHRASE armored signing key and passphrase, read
#                                       by maven-gpg-plugin's BouncyCastle signer
#
# Extra Maven options go in MAVEN_ARGS.
#
# Written for POSIX sh, so it runs with macOS's /bin/sh and on Linux.

set -eu

cd "$(dirname "$0")/.."

usage() {
    echo "usage: scripts/publish.sh snapshot|release" >&2
    exit 2
}

[ "$#" -eq 1 ] || usage
mode="$1"
case "$mode" in
    snapshot | release) ;;
    *) usage ;;
esac

version="$(./mvnw -q org.apache.maven.plugins:maven-help-plugin:3.5.1:evaluate \
    -Dexpression=project.version -DforceStdout)"

case "$mode:$version" in
    snapshot:*-SNAPSHOT)
        ;;
    snapshot:*)
        echo "Version $version is not a -SNAPSHOT version; use 'scripts/publish.sh release'" >&2
        exit 1
        ;;
    release:*-SNAPSHOT)
        echo "Version $version is a -SNAPSHOT version; set the release version or use 'scripts/publish.sh snapshot'" >&2
        exit 1
        ;;
    release:*)
        ;;
esac

exec ./mvnw -s .mvn/central-settings.xml -B -Prelease -Dgpg.signer=bc clean deploy
