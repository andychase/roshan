#!bash
cd /cygdrive/c/Users/a5c/Desktop/li/littleisland/src/roshan/buffer
protoc --java_out=../.. msg.proto
protoc-as3 --as3_out=../../../../li_client/src msg.proto
