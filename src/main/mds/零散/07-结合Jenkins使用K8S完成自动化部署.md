# DevOps的引出

## DevOps解决了什么问题

传统的项目开发模式中，往往有开发、测试、运维团队共同参与。作为开发，根据需求进行功能开发，从功能层面保证业务的正确性；在敏捷开发的模式下，功能往往会快速迭代，久而久之整个系统会有不稳定性。作为运维，更专注如何保证系统安全、稳定地运行，这种工作与业务功能是隔离开的，可以理解为业务功能的运行基石。

`开发`和`运维`作为两个部分，在迭代一个需求的时候，往往是 开发完成代码、自测，配合测试人员完成缺陷修复，再将代码打包转发给运维团队，运维人员将包部署到特定环境上。这个流程看似完美，但是在敏捷开发的场景下，需求需要快速地迭代上线，开发过程中需要不断的发版、测试、修复缺陷、再发版、再测试....如果每次发版都要运维团队部署，会导致时间成本、沟通成本变大。

而DevOps是为了解决`开发` 与`运维`的沟通成本、时间成本带来的影响而存在的，它能够使项目开发周期更加流畅。

## DevOps的流程

完整的开发流程一般包含以下步骤：

	1. PLAN：需求规划、分析、流程设计、编码设计。
	1. CODE：编写代码、完成功能。代码本身需要放在仓库。
	1. BUILD：代码构建、打包，形成一个可执行的程序。
	1. TEST：构建项目后，需要检查代码里是否存在BUG。
	1. DEPLOY：认为项目达到部署要求，准备进行部署。
	1. OPERATE：部署项目到特定环境。
	1. MONITOR：持续监控运行在特定环境的系统。

而DevOps，则是将这几个步骤通过特定工具串联起来，自动化执行，形成一个闭环，这个工具最典型的代表就是Jenkins，Jenkins管家的图标也预示着开发人员只要配置好相关流水线，这位“管家”就能帮你自动化解决这套流程：

![00](07-结合Jenkins使用K8S完成自动化部署.assets/00.jpg)

比如说我接下来打算 拉取代码、通过maven将代码打包成可执行jar包、将jar包封装成镜像、将镜像上传到Harbor、通过K8S拉取镜像、部署成Deployment这些部署，统统在Jenkins配置好，让它帮我自动化完成。

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

## java jar部署

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

## 镜像部署

准备好Dockerfile内容：

```dockerfile
FROM java:8
EXPOSE 8001
ADD Cloud_Order-1.0-SNAPSHOT.jar /
ENTRYPOINT ["java","-jar","-Dfile-encoding=utf-8 -Xmx100M","/Cloud_Order-1.0-SNAPSHOT.jar"]
```

```dockerfile
FROM java:8
EXPOSE 9001
ADD Cloud_Stock-1.0-SNAPSHOT.jar /
ENTRYPOINT ["java","-jar","-Dfile-encoding=utf-8 -Xmx100M","/Cloud_Stock-1.0-SNAPSHOT.jar"]
```

将dockerfile内容放到项目目录下的dockerfiles文件夹内，同时在原有Transfer Set上新增各自的Dockerfile（注意用,分隔）：

![13](07-结合Jenkins使用K8S完成自动化部署.assets/13.png)

Build项目，可以发现Ubuntu02内，起码Dockerfile是被传过来了：

```
kjg1@ubuntu02:~/jenkins_output$ tree .
.
├── Cloud_Order
│   ├── dockerfiles
│   │   └── Dockerfile
│   ├── sh
│   │   └── start-order.sh
│   └── target
│       ├── Cloud_Order-1.0-SNAPSHOT.jar
│       └── order.log
├── Cloud_Stock
│   ├── dockerfiles
│   │   └── Dockerfile
│   ├── sh
│   │   └── start-stock.sh
│   └── target
│       ├── Cloud_Stock-1.0-SNAPSHOT.jar
│       └── stock.log
└── Stock_Feign
    └── target
        └── Stock_Feign-1.0-SNAPSHOT.jar

10 directories, 9 files
```

