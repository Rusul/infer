alias purge="curl http://localhost:8080/infer/update?commit=true -H \"Content-Type: text/xml\" --data-binary '<delete><query>*:*</query></delete>'" 
alias purge2="curl http://localhost:8080/infer/update?commit=true -H \"Content-Type: text/xml\" --data-binary '<delete> <query>subject_t:*John*Frank*</query> <query>predicate_t:*memberOf</query> </delete>'"

alias optimize="curl 'http://localhost:8080/feast/update?optimize=true'"

alias ca="ssh azureuser@ieprod.cloudapp.net"
alias stage="ssh azureuser@iestag.cloudapp.net"
alias am="ssh ubuntu@54.227.247.129"
alias am2="ssh ubuntu@54.221.248.148"
alias am3="ssh ubuntu@54.225.193.188"
alias am4="ssh ubuntu@54.204.8.159"
alias ho="ssh -i ~/Desktop/bpadev2.pem ubuntu@git.hoyoslabs.com"
alias ho2="ssh -i ~/Desktop/bpadev2.pem  ubuntu@uat.hoyoslabs.com"
alias ho3="ssh -i ~/Desktop/bpadev2.pem  ubuntu@184.72.154.93"
alias cc="ssh ubuntu@54.212.36.102"




alias cs="ssh -p 8888 builder@www.earthsalesgroup.com" 

alias cf="ssh -p 8888 ftpuser@www.earthsalesgroup.com"
alias ch="ssh ubuntu@54.208.81.203  "

alias fixMonitor="rm ~/.config/monitors.xml"

alias avi="mencoder -nosound -ovc lavc -lavcopts vcodec=mpeg4 -o ../test.avi -mf type=jpeg:fps=0.1 mf://*"

alias sg="cd ~/ga; dev_appserver.py -a 0.0.0.0  ./"

alias fh=" hadoop namenode -format"

alias mcp=" cp -ruf ~/Music ~/Videos /netdrive "

alias css=" cp  ~/solrstore/trunk/solrstore/dist/sol*.jar ~/feast/trunk/feast/war-lib;  cp  ~/solrstore/trunk/solrstore/dist/sol*.jar ~/feast/trunk/feast/util-lib/jena-2.7.4;   "

alias st=" rm ~/logs/* ; cd ~/pkg/tomcat/; rm -rf webapps/intrud*  webapps/infer/* webapps/websec* work/* logs/* temp/*; cd bin; ./catalina.sh run "
alias stb=" rm ~/logs/* ; cd ~/pkg/tomcat/; rm -rf webapps/intrud*  webapps/infer/* webapps/websec* work/* logs/* temp/*; cd bin; ./catalina.sh start "

alias echosign=" cd  ~/Desktop/mcg/demo/php; php demo.php http://www.echosign.com/services/EchoSignDocumentService15?wsdl ZZ2VS88Y4FW55U docs "

alias dottgz="(cd ~; tar cvfz dot.tgz .[a-zA-Z0-9]*) "

alias stm=" rm ~/solr*.log; cd ~/pkg/apache-tomcat-7.0.27/; rm -rf webapps/smp* work/* logs/* temp/*; cd bin;    ./catalina.sh start "

alias gcw="git clone https://scott:lakers@git.scottstreit.com/git/websec.git"

alias short="avconv -i mib.mp4  -vcodec copy -acodec copy -ss 14 mib2.mp4"

alias gi="git --bare init"
alias dosformat="mkdosfs -F 32 -I /dev/sdb1"
alias usblabel='sudo mlabel -i /dev/sdb1 -s ::"Video"  '

alias zkcli='cd ~/pkg/solrDist/scripts/cloud-scripts;./zkcli.sh -cmd upconfig -zkhost localhost:9080 -confdir ~/pkg/solrDist/instance/solr.solr.home/infer/conf/ -confname infer '





