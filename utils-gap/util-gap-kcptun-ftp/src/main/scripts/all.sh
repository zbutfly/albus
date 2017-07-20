#!/bin/bash
target="10.118.159.44:3306"
locate="10.118.159.44:9999"

kcp_listen_port=29991
gap_port=29992

reqs=./pool/reqs
reps=./pool/reps

# 1. start invoker
sh ./run.sh Invoker ${kcp_listen_port} ${reqs} ${reps} &
# 2. start kcptun server
./kcptun/server_linux_amd64.bin -t ${target} -l ":${kcp_listen_port}" -mode fast2 &

# 3. start dispatcher
sh ./run.sh Dispatcher ${gap_port} ${reps} ${reqs} &
# 4. start kcptun client
./kcptun/client_linux_amd64.bin -r ":${gap_port}" -l "${locate}" -mode fast2 &

# stop all
#for pid in `ps aux | egrep kcptun | grep -v grep | awk '{print $2}'`; do kill -9 ${pid}; done
