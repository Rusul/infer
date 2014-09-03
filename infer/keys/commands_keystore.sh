
 openssl pkcs12 -export -in STAR_scottstreit_com.crt -inkey scottstreit.key -CAfile STAR_scottstreit_com.ca-bundle -out sample.p12

 keytool -importkeystore -srckeystore sample.p12 -destkeystore  scottstreit.keystore -srcstoretype pkcs12



