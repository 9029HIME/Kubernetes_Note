FROM java:8
EXPOSE 8001
ADD Cloud_Order-1.0-SNAPSHOT.jar /
ENTRYPOINT ["java","-jar","-Dfile-encoding=utf-8 -Xmx100M","/Cloud_Order-1.0-SNAPSHOT.jar"]
