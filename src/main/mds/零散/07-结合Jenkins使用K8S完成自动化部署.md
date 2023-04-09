# 搭建Jenkins

0. 先保证安装、配置好JDK17。

1. 下载jenkins的war包，根据JDK版本选择最优的LTS。

```
https://get.jenkins.io/war-stable/2.346.1/
```

2. 新建Jenkins目录，将war包复制到目录下。

```bash
root@kjg-PC:~# mkdir -p /usr/local/jenkins
root@kjg-PC:~# mv /home/kjg/Downloads/jenkins.war /usr/local/jenkins
root@kjg-PC:~# mkdir -p /usr/local/jenkins/logs
```

3. 编写启动脚本，启动jenkins。

```bash
root@kjg-PC:~# cd /usr/local/jenkins
root@kjg-PC:~# vim jenkins.sh

#!/bin/bash
export JENKINS_HOME=/usr/local/jenkins
cd $JENKINS_HOME
nohup java -Dhudson.model.DownloadService.noSignatureCheck=true -Xmx2g -jar jenkins.war --httpPort=9999 > logs/jenkins.log 2>&1 &
tail -f logs/jenkins.log

root@kjg-PC:~# sh jenkins.sh
```

4. 访问http://localhost:9999	按照提示完成初始化操作。

5. 配置代理，下载插件

   默认情况下插件会大范围下载失败，因此需要配置国内代理（http://mirror.esuni.jp/jenkins/updates/update-center.json）：

   ![01](07-结合Jenkins使用K8S完成自动化部署.assets/01.png)

6. 配置JDK和Git（按照本机的路径）

   ![02](07-结合Jenkins使用K8S完成自动化部署.assets/02.png)

![03](07-结合Jenkins使用K8S完成自动化部署.assets/03.png)

7. 配置SSH（**拉到最下面，这里我发现配本机会连接失败，所以最后配了另一台机子ubuntu02**）

   ![04](07-结合Jenkins使用K8S完成自动化部署.assets/04.png)

8. 添加凭据，方便后续Jenkins从Git托管平台上拉取代码：

   ![05](07-结合Jenkins使用K8S完成自动化部署.assets/05.png)
   
   9. 下载JDK Parameter插件，保证可以使用低版本JDK构建项目
   
      ![06](07-结合Jenkins使用K8S完成自动化部署.assets/06.png)

# Jenkins部署Cloud-Order、Cloud-Stock

1. 新建一个Maven项目：

   ![07](07-结合Jenkins使用K8S完成自动化部署.assets/07.png)
   
2. 通过JDK Parameter插件，指定项目构建的JDK版本为上面配置的JDK8

   ![08](07-结合Jenkins使用K8S完成自动化部署.assets/08.png)

3. 指定源码git，凭据用上面配置好的GitHub凭据

   ![09](07-结合Jenkins使用K8S完成自动化部署.assets/09.png)

4. 在Post Steps阶段传输构建好的Jar包到Ubuntu02

   ![10](07-结合Jenkins使用K8S完成自动化部署.assets/10.png)

5. 在Post Steps阶段将Cloud-Order、Cloud-Stock的启动脚本传输到Ubuntu02

   ![11](07-结合Jenkins使用K8S完成自动化部署.assets/11.png)

   ![12](07-结合Jenkins使用K8S完成自动化部署.assets/12.png)

6. 顺便看一下Cloud-Order和Cloud-Stock的启动脚本，逻辑大差不差，只是变量的区别，都是判断PID是否存在，决定在nohup java -jar之前是否kill：

   ```bash
   #!/bin/bash
   APP_NAME=Cloud_Order-1.0-SNAPSHOT.jar
   APP_LOG=order.log
   APP_HOME=/home/kjg1/jenkins_output/Cloud_Order/target
   pid=`ps -ef | grep ${APP_NAME} | grep -v grep | awk '{print $2}'`
   
   # 判断Cloud-Order是否运行中，存在：返回1，不存在：返回0
   exist() {
     if [ -z "${pid}" ]
     then
           return 0
     else
           return 1
     fi
   }
   
   stop() {
           exist
           if [ $? -eq "1" ]; then
                   kill ${pid}
                   rm -f $APP_HOME/$APP_LOG
           else
                   echo "not running"
           fi
   }
   
   start(){
           nohup /usr/local/jdk/jdk1.8.0_311/bin/java -jar -Dfile.encoding=utf-8 $APP_HOME/$APP_NAME  > $APP_HOME/$APP_LOG 2>&1 &
   }
   
   restart(){
           stop
           start
   }
   
   restart
   ```

   ```bash
   #!/bin/bash
   APP_NAME=Cloud_Stock-1.0-SNAPSHOT.jar
   APP_LOG=stock.log
   APP_HOME=/home/kjg1/jenkins_output/Cloud_Stock/target
   pid=`ps -ef | grep ${APP_NAME} | grep -v grep | awk '{print $2}'`
   
   # 判断Cloud-Order是否运行中，存在：返回1，不存在：返回0
   exist() {
     if [ -z "${pid}" ]
     then
           return 0
     else
           return 1
     fi
   }
   
   
   stop() {
           exist
           if [ $? -eq "1" ]; then
                   kill ${pid}
                   rm -f $APP_HOME/$APP_LOG
           else
                   echo "not running"
           fi
   }
   
   start(){
           nohup /usr/local/jdk/jdk1.8.0_311/bin/java -jar -Dfile.encoding=utf-8 $APP_HOME/$APP_NAME  > $APP_HOME/$APP_LOG 2>&1 &
   }
   
   
   restart(){
           stop
           start
   }
   
   restart
   ```

   **值得注意的是：判断pid是否存在的时候，要基于${APP_HOME}进行grep，而不是简单的Order或者Stock，否则会kill掉脚本本身。**