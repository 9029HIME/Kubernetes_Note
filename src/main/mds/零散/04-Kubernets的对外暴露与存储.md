# Kubernetes的“注册中心”-Service

## 概念

如果说Deploy是对Pods的部署功能进行一层封装，那么Service就是对Pods的网络功能进行了一层封装。如图所示：

![01](04-Kubernets的对外暴露与存储.assets/01.png)

现在这个Kubernetes集群部署了一个名为my-dep的Deploy，正常来说可以通过每个Pod的IP进行访问，但是这样太麻烦了，**像上图那样有3个Pod1，岂不是要访问3个不同的IP？**

因此引入了Service的概念，作为Deploy的封装，我通过命令kubectl expose deployment my-dep --port=8000 --target-port=80 --type=ClusterIP后，是这样的：

![02](04-Kubernets的对外暴露与存储.assets/02.png)

会创建出一个name=my-dep的Service，它的端口是8080，并且会产生一个CLUSTER-IP=10.98.6.6。这有什么作用？看下图：

![03](04-Kubernets的对外暴露与存储.assets/03.png)

假设现在Kubernetes内有一个Pod3，Pod3可以通过以下两种方式访问Service，从而对Pod1实现负载均衡、健康返回的访问：

1. curl http://${ClusterIP}:${Port}，即curl http://10.98.6.6:8000。
2. curl ${服务名}.${服务所在的名称空间}.svc:${端口号}，即curl http://my-dep.default.svc:8080。

负载均衡能理解，那什么是健康返回？其实就是Service会监控每个Pod1的状态，如果其中一个Pod1挂了，那么在Pod1自愈 或 故障转移 之前，Service不会将请求转发到这个挂了的Pod1。

**值得注意的是：ClusterIP模式的Service，只允许Kubernetes环境内的容器进行访问。**如果在Kubernetes环境外（比如物理机本身、物理机以外的请求）进行访问，是不允许的。

那有什么好办法呢？我总不能整个环境都部署到Kubernetes环境里吧？这时候可以用NodePort模式：

![04](04-Kubernets的对外暴露与存储.assets/04.png)

创建Service的语句基本一样，只是从--type=ClusterIP改成--type=NodePort。此时Service会随机选择一个端口进行暴露，环境内的访问方式不变。但是环境外可以通过${物理机IP}+${随机端口}的方式进行访问。当然，前提是物理机本身对外暴露了这个端口。

**从访问方式上看，可以理解为，NodePort模式是ClusterIP的一个增强。**

## 实践


