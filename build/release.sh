#!/usr/bin/env bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

VERSION=$1
ARTIFACT=$2

if [[ -z $ARTIFACT ]]
then
    echo "No Artifact Specified"
fi

SBT_2_11="sbt ++2.11.12 -Dquill.macro.log=false -Dquill.scala.version=2.11.12"
SBT_2_12="sbt ++2.12.6 -Dquill.macro.log=false -Dquill.scala.version=2.12.6"
SBT_2_13="sbt ++2.13.2 -Dquill.macro.log=false -Dquill.scala.version=2.13.2"

if [[ $VERSION -eq 211 ]]
then
    SBT_VER=$SBT_2_11
elif [[ $VERSION -eq 212 ]]
then
    SBT_VER=$SBT_2_12
elif [[ $VERSION -eq 213 ]]
then
    SBT_VER=$SBT_2_13
else
    echo "No Valid SBT Version Entered"
    exit 1
fi

echo $SBT_CMD
if [[ $TRAVIS_PULL_REQUEST == "false" ]]
then
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt.temp -d
    openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    gpg --import local.secring.gpg
    gpg --import local.pubring.gpg
    gpg --list-keys
    for fpr in $(gpg --list-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u); do  echo -e "5\ny\n" |  gpg --command-fd 0 --expert --edit-key $fpr trust; done

    # Temp hack to get around SbtPgp being moved. This file needs to be updated with correct import.
    # See https://github.com/sbt/sbt-pgp/pull/162 for more details
    cat local.credentials.sbt.temp | sed 's/import com.typesafe.sbt.SbtPgp/import com.jsuereth.sbtpgp.SbtPgp/' > local.credentials.sbt

    ls -ltr
    sleep 3 # Need to wait until credential files fully written or build fails sometimes
    project_version="$(sbt 'show version' | tail -1 | cut -f 2)"
    echo "Detected Project Version $project_version from SBT Files"

    # When an artifact is actually published, a build will go out on the git commit: "Setting version to <YOUR VERSION>".
    # At that point the $TRAVIS_BRANCH branch varialbe will be set to "v<YOUR VERSION>" which should be the same as
    # the version in version.sbt (that's what the $project_version variable is. That's the commit on which
    # we want to do the actual project release!.... as well as any branch name 're-release*' in case a build fails and we need to re-publish.
    # (Also note, we could technically use $project_version instead of $(cat version.sbt) but I don't want to change that this time around.)

    if [[ ($TRAVIS_BRANCH == "v${project_version}" || $TRAVIS_BRANCH == "re-release"*) && $(cat version.sbt) != *"SNAPSHOT"* ]]
    then
        echo "Release Build for $TRAVIS_BRANCH"
        eval "$(ssh-agent -s)"
        chmod 600 local.deploy_key.pem
        ssh-add local.deploy_key.pem
        git config --global user.name "Quill CI"
        git config --global user.email "quillci@getquill.io"
        git remote set-url origin git@github.com:getquill/quill.git

        if [[ $ARTIFACT == "base" ]]; then    $SBT_VER -Dmodules=base -DskipPush=true 'release with-defaults'; fi
        if [[ $ARTIFACT == "db" ]]; then      $SBT_VER -Dmodules=db -DskipPush=true 'release with-defaults'; fi
        if [[ $ARTIFACT == "js" ]]; then      $SBT_VER -Dmodules=js -DskipPush=true 'release with-defaults'; fi
        if [[ $ARTIFACT == "async" ]]; then   $SBT_VER -Dmodules=async -DskipPush=true 'release with-defaults'; fi
        if [[ $ARTIFACT == "codegen" ]]; then $SBT_VER -Dmodules=codegen -DskipPush=true 'release with-defaults'; fi
        if [[ $ARTIFACT == "bigdata" ]]; then $SBT_VER -Dmodules=bigdata -DskipPush=true 'release with-defaults'; fi

        # Publish Everything
        if [[ $ARTIFACT == "publish" ]]; then $SBT_VER -Dmodules=none 'release with-defaults default-tag-exists-answer o'; fi

    elif [[ $TRAVIS_BRANCH == "master" && $(cat version.sbt) == *"SNAPSHOT"* ]]
    then
        echo "Master Non-Release Build for $TRAVIS_BRANCH"
        if [[ $ARTIFACT == "base" ]]; then    $SBT_VER -Dmodules=base publish; fi
        if [[ $ARTIFACT == "db" ]]; then      $SBT_VER -Dmodules=db publish; fi
        if [[ $ARTIFACT == "js" ]]; then      $SBT_VER -Dmodules=js publish; fi
        if [[ $ARTIFACT == "async" ]]; then   $SBT_VER -Dmodules=async publish; fi
        if [[ $ARTIFACT == "codegen" ]]; then $SBT_VER -Dmodules=codegen publish; fi
        if [[ $ARTIFACT == "bigdata" ]]; then $SBT_VER -Dmodules=bigdata publish; fi

        # No-Op Publish
        if [[ $ARTIFACT == "publish" ]]; then echo "No-Op Publish for Non Release Master Branch"; fi

    elif [[ $TRAVIS_BRANCH == "master" && $(cat version.sbt) != *"SNAPSHOT"* ]]
    then
        echo "Publish is No-Op for Master Preliminary Release Build for $ARTIFACT. Actual release will happen in 'Setting version to...' build"
    else
        echo "Branch build for $TRAVIS_BRANCH"
        echo "version in ThisBuild := \"$TRAVIS_BRANCH-SNAPSHOT\"" > version.sbt
        if [[ $ARTIFACT == "base" ]]; then    $SBT_VER -Dmodules=base publish; fi
        if [[ $ARTIFACT == "db" ]]; then      $SBT_VER -Dmodules=db publish; fi
        if [[ $ARTIFACT == "js" ]]; then      $SBT_VER -Dmodules=js publish; fi
        if [[ $ARTIFACT == "async" ]]; then   $SBT_VER -Dmodules=async publish; fi
        if [[ $ARTIFACT == "codegen" ]]; then $SBT_VER -Dmodules=codegen publish; fi
        if [[ $ARTIFACT == "bigdata" ]]; then $SBT_VER -Dmodules=bigdata publish; fi

        # No-Op Publish
        if [[ $ARTIFACT == "publish" ]]; then echo "No-Op Publish for Non Release Snapshot Branch"; fi
    fi
fi
