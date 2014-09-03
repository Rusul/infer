export ANT_OPTS="-Xms512m -Xmx512m"
export JAVA_HOME="${PKG_HOME}/jdk"

export ANT_HOME="${PKG_HOME}/ant"
complete -C ${ANT_HOME}/bin/complete-ant-cmd.pl ant build.sh

export MVN_HOME="${PKG_HOME}/ant"

export CATALINA_BASE=${PKG_HOME}/tomcat

export MAVEN_HOME="${PKG_HOME}/maven"

export JBOSS_USER=scott

export JAVAPATH=${HOME}/pkg/jdk
export REP=infer
export PROJECT=infer
export ANDROID_HOME="${PKG_HOME}/adt-bundle-linux-x86_64-20131030/sdk"
export ANDROID_NDK="${PKG_HOME}/android-ndk-r9c"
#export JAVA_OPTS=" -Djava.awt.headless=true -Xmx2048m  -XX:PermSize=128m"

PATH="${PKG_HOME}/bin:${JAVA_HOME}/bin:${ANT_HOME}/bin:${PKG_HOME}/eclipse/bin:${PKG_HOME}/protege:${HADOOP_INSTALL}/bin:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${ANDROID_NDK}:${MAVEN_HOME}/bin:${PATH}"

export PATH
export JETTY_HOME=${HOME}/pkg/jetty-hightide-8.0.0.v20110901

#export JAVA_OPTS="$JAVA_OPTS -DzkRun -Djetty.port=8080 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home  -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m   -Dbootstrap_conf=true "

#export JAVA_OPTS="$JAVA_OPTS -DzkRun -Djetty.port=8080 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home  -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m   -Dbootstrap_conf=true -Dzookeeper=localhost:9080 -Dcollection=infer -Dids=ids:8080"

export JAVA_OPTS="-DzkHost=localhost:2181 -Djetty.port=8080 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home  -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m  -Dzookeeper=localhost:2181 -Dcollection=infer -Dids=ids:8080 "


#export JAVA_OPTS="$JAVA_OPTS -DzkRun -Djava.security.auth.login.config=$CATALINA_BASE/conf/jaas.config  -Djetty.port=8080 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home  -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m   -Dbootstrap_conf=true "


#export JAVA_OPTS="$JAVA_OPTS -Djetty.port=8080 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m  -DzkRun -Dbootstrap_conf=true "

#export JAVA_OPTS="$JAVA_OPTS -DzkRun -Djetty.port=8080   -DnumShards=2   -Dshard=shard1 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx1024m  -XX:PermSize=256m  -Dbootstrap_conf=true  "

#export JAVA_OPTS="$JAVA_OPTS  -Djetty.port=8080  -Dhost=companion1 -DzkHost=companion1:2181 -Dsolr.solr.home=${PKG_HOME}/solrDist/instance/solr.solr.home -Djava.util.logging.config.file=${HOME}/${REP}/${PROJECT}/logging.properties -Xmx6000m -Xms6000m -XX:PermSize=256m    -Dbootstrap_conf=true  "

export ZOO_LOG_DIR=${PKG_HOME}/zookeeper/logs
export JVMFLAGS="-Xmx1024m  -XX:PermSize=256m"

export JAVA_OPTIONS=$JAVA_OPT
export JENKINS_HOME=${HOME}/pkg/jenkins
#export UBUNTU_MENUPROXY=0
umask 2
