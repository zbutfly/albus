#!/bin/bash
set -o nounset
set -o errexit
export LANG=zh_CN.UTF-8

export JAVA_HOME=" /usr/java/jdk1.8.0_60/"
export PATH=${JAVA_HOME}/bin:${PATH}
java -version

TSTR=`date +"%Y%m%d%H%M%s"`
_GC_LOG="./logs/gc${TSTR}.log"

_GC_G1="-XX:+UnlockExperimentalVMOptions \
-XX:+DisableExplicitGC \
-XX:+UseG1GC \
-XX:+AggressiveOpts \
-XX:MaxGCPauseMillis=200 \
-XX:G1HeapRegionSize=16M \
-XX:InitiatingHeapOccupancyPercent=35 \
-XX:G1NewSizePercent=20 \
-XX:G1MaxNewSizePercent=30 \
-XX:CompileThreshold=8000 \
-XX:+TieredCompilation \
-XX:ParallelGCThreads=20 \
-XX:ConcGCThreads=10 \
-XX:+UseFastAccessorMethods \
-XX:+UseFastEmptyMethods \
-XX:+UseFastJNIAccessors \
-XX:+UseStringDeduplication \
-XX:+OptimizeStringConcat \
-XX:NewRatio=10 \
-Djdk.map.althashing.threshold=0"

_GC_INFO="-XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
-XX:+PrintGCApplicationStoppedTime -Xloggc:${_GC_LOG} \
-XX:+PrintStringDeduplicationStatistics \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=."

#echo GC Log File: ${_GC_LOG}
#_GC_G1="${_GC_G1} ${_GC_INFO}"

if [ -z ${JAVA_OPTS+x} ]; then JAVA_OPTS=; else echo "Original JAVA_OPTS: ${JAVA_OPTS}"; fi
JAVA_OPTS="${JAVA_OPTS} ${_GC_G1} -d64 -server"
echo JAVA_OPTS:	${JAVA_OPTS}

_CP="./test-classes:./albus-util-gap-http.jar:./dependency/*"
echo "CLASSPATH:	${_CP}"
_MAIN=$1
_LOG="./logs/${_MAIN,,}-${TSTR}.log"
shift 1
_MAIN="net.butfly.bus.utils.gap.${_MAIN}"
echo "MAIN_CLASS:	${_MAIN}"
echo "      ARGS:	$*"
#JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=18082"
_CMD="java ${JAVA_OPTS} -cp ${_CP} ${_MAIN} $*"
echo "   ALL_CMD:	${_CMD}"
echo "  LOG_FILE:	${_LOG}"
${_CMD} > ${_LOG} 2>&1 &
#tail -fn100 ${_LOG}

#./run.sh Invoker 192.168.22.23 6000 6002 ./pool/reps ./pool/reqs
#./run.sh Dispatcher 0.0.0.0 6001 6003 ./pool/reqs ./pool/reps
