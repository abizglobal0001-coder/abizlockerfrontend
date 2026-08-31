FROM tomcat:9.0-jdk21-temurin

# Remove default Tomcat apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy ABIZ LOCKER launcher.war as ROOT.war so it serves at root /
COPY ABIZ-LOCKER-RELEASE-22.0.0/backend/launcher.war /usr/local/tomcat/webapps/ROOT.war

# Expose default port
EXPOSE 8080

CMD ["catalina.sh", "run"]
