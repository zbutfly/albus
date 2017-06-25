.\server_windows_amd64 -t "115.239.211.112:80" -l ":29991" -mode fast2
.\client_windows_amd64 -r "127.0.0.1:29992" -l ":29990" -mode fast2
java -d64 -server -cp ./test-classes;./albus-util-gap-udp-simple.jar;./dependency/* net.butfly.bus.utils.gap.Dispatcher 29992 ???
java -d64 -server -cp ./test-classes;./albus-util-gap-udp-simple.jar;./dependency/* net.butfly.bus.utils.gap.Invoker ??? 29991  
