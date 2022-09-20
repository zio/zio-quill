#!/usr/bin/env bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

VERSION=$1
ARTIFACT=$2

echo "Begin Release Script for BRANCH=$BRANCH VERSION=$VERSION ARTIFACT=$ARTIFACT"

if [[ -z $ARTIFACT ]]
then
    echo "No Artifact Specified"
fi

SBT_2_12="sbt ++2.12.17 -Dquill.macro.log=false -Dquill.scala.version=2.12.17"
SBT_2_13="sbt ++2.13.10 -Dquill.macro.log=false -Dquill.scala.version=2.13.10"
SBT_3_2="sbt ++3.2.2 -Dquill.macro.log=false -Dquill.scala.version=3.2.2"

if [[ $VERSION -eq 212 ]]
then
    SBT_VER=$SBT_2_12
elif [[ $VERSION -eq 213 ]]
then
    SBT_VER=$SBT_2_13
elif [[ $VERSION -eq 32 ]]
then
    SBT_VER=$SBT_3_2
else
    echo "No Valid SBT Version Entered"
    exit 1
fi

echo "$SBT_VER"
if [[ $PULL_REQUEST == "false" ]]
then
    echo "Export secring"
    openssl aes-256-cbc -md sha256 -salt -pbkdf2 -pass pass:$ENCRYPTION_PASSWORD -in ./build/secring.gpg.enc -out local.secring.gpg -d
    echo "Export pubring"
    openssl aes-256-cbc -md sha256 -salt -pbkdf2 -pass pass:$ENCRYPTION_PASSWORD -in ./build/pubring.gpg.enc -out local.pubring.gpg -d
    echo "Export creds"
    openssl aes-256-cbc -md sha256 -salt -pbkdf2 -pass pass:$ENCRYPTION_PASSWORD -in ./build/credentials.sbt.enc -out local.credentials.sbt -d
    echo "Export key"
    openssl aes-256-cbc -md sha256 -salt -pbkdf2 -pass pass:$ENCRYPTION_PASSWORD -in ./build/deploy_key.pem.enc -out local.deploy_key.pem -d

    ls -ltr

    gpg --version

    echo "Import pubring"
    gpg --import --batch local.pubring.gpg
    echo "Import secring"
    gpg --import --batch local.secring.gpg
    echo "List keys"
    gpg --list-keys

    #echo "Set to trust"
    #echo "Trust Keys"

    # Need to specify to trust GPG keys. Answer '5' (ultimate trust) to "Please decide how far you trust this user" and then 'y' to acknowledge that
    for fpr in $(gpg --list-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u); do echo -e "5\ny\n" |  gpg  --command-fd 0 --status-fd 2 --batch  --expert --edit-key $fpr trust; done
    # Same for secret keys
    for fpr in $(gpg --list-secret-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u); do echo -e "5\ny\n" |  gpg  --command-fd 0 --status-fd 2 --batch  --expert --edit-key $fpr trust; done


    ls -ltr
    sleep 3 # Need to wait until credential files fully written or build fails sometimes
    project_version="v$(cat version.sbt | awk -F'"' '{print $2}')"
    echo "Detected project_version '$project_version' from SBT Files (on BRANCH '$BRANCH')"

    # When an artifact is actually published, a build will go out on the git commit: "Setting version to <YOUR VERSION>".
    # The job before that is the one that creates the vX.X.X tag e.g. v3.0.0. We build and release on that one
    # as well as any branch name 're-release*' in case a build fails and we need to re-publish.
    # (Also note, we could technically use $project_version instead of $(cat version.sbt) but I don't want to change that this time around.)

    if [[ ($BRANCH == "master" || $BRANCH == "re-release"*) && $(cat version.sbt) != *"SNAPSHOT"* ]]
    then
        echo "Release Build for $BRANCH - Artifact: '$ARTIFACT'"
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

        # Commit next version and tag if we are on the master branch (i.e. not if we are on a re-release)
        if [[ $BRANCH == "master" && $ARTIFACT == "publish" ]]; then
          echo "Doing Master Publish for BRANCH=$BRANCH VERSION=$VERSION ARTIFACT=$ARTIFACT"
          # Delete the website tag. If it does not currently exist then ignore it.
          git push --delete origin website || true
          $SBT_VER -Dmodules=none 'release with-defaults default-tag-exists-answer o';
        fi

    elif [[ $BRANCH == "master" && $(cat version.sbt) == *"SNAPSHOT"* ]]
    then
        echo "Master Non-Release Build for $BRANCH - Artifact: '$ARTIFACT'"
        if [[ $ARTIFACT == "base" ]]; then    $SBT_VER -Dmodules=base publish; fi
        if [[ $ARTIFACT == "db" ]]; then      $SBT_VER -Dmodules=db publish; fi
        if [[ $ARTIFACT == "js" ]]; then      $SBT_VER -Dmodules=js publish; fi
        if [[ $ARTIFACT == "async" ]]; then   $SBT_VER -Dmodules=async publish; fi
        if [[ $ARTIFACT == "codegen" ]]; then $SBT_VER -Dmodules=codegen publish; fi
        if [[ $ARTIFACT == "bigdata" ]]; then $SBT_VER -Dmodules=bigdata publish; fi

        # No-Op Publish
        if [[ $ARTIFACT == "publish" ]]; then echo "No-Op Publish for Non Release Master Branch"; fi

    # If we are a branch build publish it. We are assuming this script does NOT become activated in pulls requests
    # and that condition is done at a higher level then this script
    elif [[ $BRANCH != "master" ]]
    then
        echo "Branch build for $BRANCH - Artifact: '$ARTIFACT'"
        echo "version in ThisBuild := \"$BRANCH-SNAPSHOT\"" > version.sbt
        if [[ $ARTIFACT == "base" ]]; then    $SBT_VER -Dmodules=base publish; fi
        if [[ $ARTIFACT == "db" ]]; then      $SBT_VER -Dmodules=db publish; fi
        if [[ $ARTIFACT == "js" ]]; then      $SBT_VER -Dmodules=js publish; fi
        if [[ $ARTIFACT == "async" ]]; then   $SBT_VER -Dmodules=async publish; fi
        if [[ $ARTIFACT == "codegen" ]]; then $SBT_VER -Dmodules=codegen publish; fi
        if [[ $ARTIFACT == "bigdata" ]]; then $SBT_VER -Dmodules=bigdata publish; fi
        if [[ $ARTIFACT == "docs" ]]; then    $SBT_VER -Dmodules=docs publish; fi

        # No-Op Publish
        if [[ $ARTIFACT == "publish" ]]; then echo "No-Op Publish for Non Release Snapshot Branch"; fi
    else
        VERSION_FILE=$(cat version.sbt)
        echo "GitHub actions branch was: ${BRANCH} and version file is $VERSION_FILE. Not Sure what to do."
    fi
else
  echo "PULL_REQUEST is not 'false' ($PULL_REQUEST). Not doing a release."
fi
