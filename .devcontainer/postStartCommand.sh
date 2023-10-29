#!/usr/bin/env bash

DIR="$( dirname -- "$BASH_SOURCE"; )";

#### maven mirror

MVN_MIRROR=https://nexus.localhost.direct/repository/maven-public/
curl -fs $MVN_MIRROR 2>&1 >/dev/null
if [ $? -eq 0 ]; then
    echo "-- maven mirror '$MVN_MIRROR' ACTIVE"
    cp -v $DIR/maven-settings.xml ~/.m2/settings.xml
else
    echo "-- kein maven cache gefunden. ok."
    if [ -f ~/.m2/settings.xml ]; then
        echo "-- ich lösche deine settings.xml, da der mirror nicht verfügbar ist"
        rm ~/.m2/settings.xml
    fi
fi

### npm mirror

NPM_MIRROR=https://nexus.localhost.direct/repository/npm/
curl -fs $NPM_MIRROR 2>&1 >/dev/null
if [ $? -eq 0 ]; then
    echo "-- npm mirror '$NPM_MIRROR' ACTIVE!"
    npm config set registry $NPM_MIRROR
else
    echo "-- kein npm cache gefunden. ok."
    npm config set registry https://registry.npmjs.com
fi

###

echo "-- wir installieren mal Angular cli für dich"
npm install -g @angular/cli
echo "-- wir sorgen dafür, dass maven lokal Dateien cachen kann"
sudo chmod ugo+w -R /home/vscode/.m2

