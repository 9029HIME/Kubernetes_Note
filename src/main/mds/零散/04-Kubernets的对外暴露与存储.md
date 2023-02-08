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

# Kubernetes的“Nginx”-Ingress

## 概念

**Ingress底层是通过nginx来实现的，可以理解为Nginx在Kubernetes的适配版，用来对Service进行再一层封装。**

有了Service的概念，再看Ingress就比较好理解了，假设有这么一个场景，以**用户、库存、订单**服务为例，每1个服务对应1个Service，每1个服务对应的实例对应N个Pods，部署在3个机子组成的Kubernetes集群里（同1种颜色代表同1个Service）：

![05](04-Kubernets的对外暴露与存储.assets/05.png)

即使前面说过，Service可以通过NodePort的方式对外暴露，在这种场景下，不同Service本质通过端口进行区分。假如前端需要访问 订单、用户、库存服务，是不是要维护3份不同的端口号？这样比较麻烦，因此可以使用Ingress对Service再进行一层封装：

![06](04-Kubernets的对外暴露与存储.assets/06.png)

新建一个名叫my-ing的Ingress，它根据/order/xxx关联订单服务、/user/xxx关联用户服务、/stock/xxx关联库存服务。同时，my-ing会选择2个随机IP，分别映射物理机的80和443端口。

![07](04-Kubernets的对外暴露与存储.assets/07.png)

这样，当外部HTTP请求通过 ${任意IP地址} + ${随机端口} + ${访问路径}的方式，经过Ingress的转发，可以到达集群内不同的Service。对于外部而言，只有固定端口，至于IP可以通过LVS来实现固定IP。

## 实践

