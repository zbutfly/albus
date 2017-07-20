rem: 1. start Invoker
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Invoker 29991 E:\pool\reqs E:\pool\reps
rem: 2. start kcp-server
.\kcptun\server_windows_amd64 -t "172.16.16.241:22" -l ":29991" -mode fast2


rem: 3. start Dispatcher
java -d64 -server -cp ./test-classes;./albus-util-gap-kcptun.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher 29992 E:\pool\reps E:\pool\reqs
rem: 4. start kcp-client
.\kcptun\client_windows_amd64 -r "127.0.0.1:29992" -l ":29990" -mode fast2


rem: 5. access ssh on 29990
ssh root@172.16.16.116 29990