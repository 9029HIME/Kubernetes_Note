FROM java:8
EXPOSE 9001
ADD Cloud_Stock-1.0-SNAPSHOT.jar /
ENTRYPOINT ["java","-jar","-Dfile-encoding=utf-8 -Xmx100M","/Cloud_Stock-1.0-SNAPSHOT.jar"]
