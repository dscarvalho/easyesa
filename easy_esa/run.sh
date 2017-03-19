#!/bin/bash

base_dir="$1"
echo "Starting MongoDB..."
mongodb/bin/mongod --dbpath "$base_dir"/mongodb/data/db > /dev/null & 
sleep 10

echo "Starting Jetty server..."
java -Xmx8g -jar easy_esa.jar 8800 index &
sleep 10

#iceweasel "http://localhost:8800/esaservice?task=esa&term1=computing&term2=sensor"&
