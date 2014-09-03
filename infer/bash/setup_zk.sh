#! /bin/bash

cd ~/pkg/zookeeper/bin

./zkServer.sh start

sleep 20;

cd ~/pkg/solrDist/scripts/cloud-scripts; ./zkcli.sh -cmd bootstrap --zkhost localhost:2181 --solrhome ~/pkg/solrDist/instance/solr.solr.home/




