FROM tomcat:9.0-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY ABIZ-LOCKER-RELEASE-22.0.0/backend/launcher.war /usr/local/tomcat/webapps/ROOT.war

RUN mkdir -p /opt/mdmesh/files /opt/mdmesh/plugins /usr/local/tomcat/conf/Catalina/localhost
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
