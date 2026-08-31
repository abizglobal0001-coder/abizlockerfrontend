FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY mdmesh/pom.xml ./
COPY mdmesh/common ./common
COPY mdmesh/jwt ./jwt
COPY mdmesh/notification ./notification
COPY mdmesh/plugins ./plugins
COPY mdmesh/server ./server
COPY mdmesh/swagger ./swagger
RUN cp server/build.properties.example server/build.properties || true
RUN mvn -q -B -DskipTests package

FROM tomcat:9.0-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /src/server/target/launcher.war /usr/local/tomcat/webapps/ROOT.war

RUN mkdir -p /opt/mdmesh/files /opt/mdmesh/plugins /usr/local/tomcat/conf/Catalina/localhost
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
