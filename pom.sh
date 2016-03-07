#!/bin/bash

<<<<<<< HEAD
mvn deploy:deploy-file -f node.pom -DpomFile=node.pom -Dfile=node.pom -Durl=http://repos.corp.butfly.co:60080/nexus/content/repositories/releases/
=======
mvn -s C:\Users\butfly\.m2\settings-local.xml -f parent.pom deploy:deploy-file -DpomFile=parent.pom -Dfile=parent.pom -Durl=http://repos.corp.hzcominfo.com:6080/nexus/content/repositories/snapshots/
>>>>>>> e3fc5a241ee3020e8f1a6e0190e781c8c7b81ff7
