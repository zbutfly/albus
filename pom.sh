#!/bin/bash

mvn deploy:deploy-file -f node.pom -DpomFile=node.pom -Dfile=node.pom -Durl=http://repos.corp.butfly.co:60080/nexus/content/repositories/releases/
