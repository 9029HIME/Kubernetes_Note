# 疑难杂症（看完后本篇后再看）

## 无法访问其他节点的Pod，提示connect refused

新建了multi-pods后，在Master通过curl访问目标地址80端口，提示connect refused。

查看Pod状态，发现calico有一个Pod没起来：

```bash
root@kjg-PC:~# kubectl get pods -A
NAMESPACE              NAME                                         READY   STATUS    RESTARTS   AGE
default                multi-pods                                   2/2     Running   0          41m
kube-system            calico-kube-controllers-5bb48c55fd-r6txn     1/1     Running   9          6d1h
kube-system            calico-node-9pxml                            1/1     Running   5          6d
kube-system            calico-node-bvrgq                            1/1     Running   5          6d
###这里没起来！！！！！！！！！！！
kube-system            calico-node-cncp8                            0/1     Running   0          10s
```

通过kubectl describe看看没起来的原因：

```bash
root@kjg-PC:~# kubectl describe pod calico-node-cncp8 -n kube-system
###省略
ect: connection refused
  Warning  Unhealthy  74s   kubelet            Readiness probe failed: 2023-02-04 02:19:30.981 [INFO][309] confd/health.go 180: Number of node(s) with BGP peering established = 0
calico/node is not ready: BIRD is not ready: BGP not established with 192.168.120.121,192.168.120.122
```

找到了核心错误原因：calico/node is not ready: BIRD is not ready: BGP not established with xxxx。

网上找到的解决方案，可以通过修改calico配置，通过DNS来找到目标节点的地址**（前提是，Master ping Node能连通！！！）**

修改calico的配置文件，找到CLUSTER_TYPE关键字，在下面新增以下配置（**其实顺序没要求，只不过这样好找**）：

```yaml
###省略上面
	containers:
	  ###省略
	  env:
	    ###省略
		-name: CLUSTER_TYPE
         value: "k8s,bgp"
         ###新增以下配置
        -name: IP_AUTODETECTION_METHOD
         value: "can-reach=114.114.114.114"
```

保存文件，重新部署到Kubernetes上，等待一会儿，可以看到calico的pod启动完毕：

```bash
root@kjg-PC:~# kubectl apply -f calico.yaml 
configmap/calico-config unchanged
customresourcedefinition.apiextensions.k8s.io/bgpconfigurations.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/bgppeers.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/blockaffinities.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/caliconodestatuses.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/clusterinformations.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/felixconfigurations.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/globalnetworkpolicies.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/globalnetworksets.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/hostendpoints.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/ipamblocks.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/ipamconfigs.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/ipamhandles.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/ippools.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/ipreservations.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/kubecontrollersconfigurations.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/networkpolicies.crd.projectcalico.org configured
customresourcedefinition.apiextensions.k8s.io/networksets.crd.projectcalico.org configured
clusterrole.rbac.authorization.k8s.io/calico-kube-controllers unchanged
clusterrolebinding.rbac.authorization.k8s.io/calico-kube-controllers unchanged
clusterrole.rbac.authorization.k8s.io/calico-node unchanged
clusterrolebinding.rbac.authorization.k8s.io/calico-node unchanged
daemonset.apps/calico-node configured
serviceaccount/calico-node unchanged
deployment.apps/calico-kube-controllers unchanged
serviceaccount/calico-kube-controllers unchanged
poddisruptionbudget.policy/calico-kube-controllers unchanged



###等待一会儿
root@kjg-PC:~# kubectl get pods -A
NAMESPACE              NAME                                         READY   STATUS    RESTARTS   AGE
default                multi-pods                                   2/2     Running   0          43m
kube-system            calico-kube-controllers-5bb48c55fd-r6txn     1/1     Running   9          6d1h
kube-system            calico-node-cncp8                            1/1     Running   0          2m13s
kube-system            calico-node-nl4mm                            1/1     Running   0          90s
kube-system            calico-node-q77lf                            1/1     Running   0          113s
kube-system            coredns-7f89b7bc75-b2r68                     1/1     Running   9          6d1h
kube-system            coredns-7f89b7bc75-kr2m4                     1/1     Running   9          6d1h
kube-system            etcd-kjg-pc                                  1/1     Running   10         6d1h
kube-system            kube-apiserver-kjg-pc                        1/1     Running   10         6d1h
kube-system            kube-controller-manager-kjg-pc               1/1     Running   11         6d1h
kube-system            kube-proxy-2hhr2                             1/1     Running   5          6d
kube-system            kube-proxy-9qdgv                             1/1     Running   9          6d1h
kube-system            kube-proxy-tkwf6                             1/1     Running   5          6d
kube-system            kube-scheduler-kjg-pc                        1/1     Running   12         6d1h
kubernetes-dashboard   dashboard-metrics-scraper-79c5968bdc-f88vn   1/1     Running   4          3d17h
kubernetes-dashboard   kubernetes-dashboard-658485d5c7-4d8ml        1/1     Running   3          3d
```

再试一下请求multi-pod的nginx容器：

```bash
root@kjg-PC:~# kubectl get pod multi-pods -owide
NAME         READY   STATUS    RESTARTS   AGE   IP             NODE       NOMINATED NODE   READINESS GATES
multi-pods   2/2     Running   0          43m   172.31.3.195   ubuntu01   <none>           <none>
root@kjg-PC:~# curl http://172.31.3.195
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

成功！

# 其他工作负载（看完本篇后再看）

1. Pod增强 除了Deployment这种工作负载外，还有其他3类不同的工作负载。不同工作负载有各自的特性。
2. Deployment适合部署无状态的应用，比如微服务实例，它的特点是**应用不存储关键数据**、提供多副本的功能。比如订单微服务，如果访问量激增，可以直接扩容应对。
3. StatefulSet适合部署有状态的应用，比如MySQL、Redis、RabbitMQ，这些应用很关心数据的一致性，因此StatefulSet会提供稳定的存储功能、网络功能。
4. DaemonSet适合部署机器守护型应用，比如日志处理、**每台机子最多部署一份实例**。
5. Job/CronJob：定时任务部署，比如垃圾清理组件，能够指定时间运行。

# 隔离资源-Namespace

## 是什么

和微服务组件-配置中心里的Namespace一样，Kubernetes的Namespace也是为了隔离资源而存在的。注意！这里的资源不包含网络，不同Namespace的pod（pod概念在后面，现在可以简单理解为Docker的容器）是可以互相访问的。以一张图为例，最直观感受Namespace在Kubernetes的存在：

![01](03-Kubernetes的进阶概念.assets/01.png)

## 查

查看kubernetes集群下的namespace：

```bash
root@kjg-PC:~# kubectl get namespace
NAME                   STATUS   AGE
default                Active   3d1h
kube-node-lease        Active   3d1h
kube-public            Active   3d1h
kube-system            Active   3d1h
kubernetes-dashboard   Active   17h
root@kjg-PC:~# kubectl get namespaces
NAME                   STATUS   AGE
default                Active   3d1h
kube-node-lease        Active   3d1h
kube-public            Active   3d1h
kube-system            Active   3d1h
kubernetes-dashboard   Active   17h
root@kjg-PC:~# kubectl get ns
NAME                   STATUS   AGE
default                Active   3d1h
kube-node-lease        Active   3d1h
kube-public            Active   3d1h
kube-system            Active   3d1h
kubernetes-dashboard   Active   17h
root@kjg-PC:~# 
```

可以看到，Kubernetes集群本身就有基础namespace，还有前面安装上去的kubernetes-dashboard。

## 增删

```bash
###增
root@kjg-PC:~# kubectl create ns kjg-namespace
namespace/kjg-namespace created
root@kjg-PC:~# kubectl get ns
NAME                   STATUS   AGE
default                Active   3d1h
kjg-namespace          Active   6s
kube-node-lease        Active   3d1h
kube-public            Active   3d1h
kube-system            Active   3d1h
kubernetes-dashboard   Active   17h

### 删
root@kjg-PC:~# kubectl delete ns kjg-namespace
namespace "kjg-namespace" deleted
root@kjg-PC:~# kubectl get ns
NAME                   STATUS   AGE
default                Active   3d1h
kube-node-lease        Active   3d1h
kube-public            Active   3d1h
kube-system            Active   3d1h
kubernetes-dashboard   Active   17h
```

像**default**这个namespace是无法删除的，最好也不要删除Kubernetes自带的namespace。

# 应用的最小单位-Pod

## 是什么

在Docker中，应用的最小单位是Container，也就是容器。

但是在Kubernetes，应用的最小单位是Pod，它管理着**运行中的一组**容器。

来看一下，以前只用Docker的话，是这样的场景：

![02](03-Kubernetes的进阶概念.assets/02.png)

每台机子安装了Docker运行环境，容器在运行环境中创建、运行。

引入了Kubernetes后，Kubernetes在容器的基础上又封装了一层Pod（注意！！！这里Kubernetes和Docker的关系可能不太准确，这里以包含关系展示）：

![03](03-Kubernetes的进阶概念.assets/03.png)

既然是**一组容器**，说明Pod能代表1个容器、多个容器。这也说明了为什么kubectl get pods -A后，Ready是以分子/分母的形式呈现：

```bash
root@kjg-PC:~# kubectl get pods -A
NAMESPACE              NAME                                         READY   STATUS        RESTARTS   AGE
kube-system            calico-kube-controllers-5bb48c55fd-r6txn     1/1     Running       6          3d2h
kube-system            calico-node-7p8wd                            1/1     Running       6          3d2h
kube-system            calico-node-9pxml                            1/1     Running       3          3d1h
kube-system            calico-node-bvrgq                            1/1     Running       3          3d1h
kube-system            coredns-7f89b7bc75-b2r68                     1/1     Running       6          3d2h
kube-system            coredns-7f89b7bc75-kr2m4                     1/1     Running       6          3d2h
kube-system            etcd-kjg-pc                                  1/1     Running       7          3d2h
kube-system            kube-apiserver-kjg-pc                        1/1     Running       7          3d2h
kube-system            kube-controller-manager-kjg-pc               1/1     Running       8          3d2h
kube-system            kube-proxy-2hhr2                             1/1     Running       3          3d1h
kube-system            kube-proxy-9qdgv                             1/1     Running       6          3d2h
kube-system            kube-proxy-tkwf6                             1/1     Running       3          3d1h
kube-system            kube-scheduler-kjg-pc                        1/1     Running       9          3d2h
kubernetes-dashboard   dashboard-metrics-scraper-79c5968bdc-f88vn   1/1     Running       1          18h
kubernetes-dashboard   kubernetes-dashboard-658485d5c7-4d8ml        1/1     Running       0          58m
kubernetes-dashboard   kubernetes-dashboard-658485d5c7-rh46b        1/1     Terminating   0          18h
```

代表这个Pod下，${分母}容器下有${分子}个容器正常运行着。

## 命令行新建Pod

先看一下节点状态是否正常：

```bash
root@kjg-PC:~# kubectl get nodes
NAME       STATUS   ROLES                  AGE    VERSION
kjg-pc     Ready    control-plane,master   3d2h   v1.20.9
ubuntu01   Ready    <none>                 3d1h   v1.20.9
ubuntu02   Ready    <none>                 3d1h   v1.20.9
```

执行kubectl run podNginx --image=nginx命令，因为没有指定ns，默认会创建在default这个名称空间：

```bash
root@kjg-PC:~# kubectl run pod-nginx --image=nginx
pod/pod-nginx created
```

值得注意的是：新建Pod，默认在default名称空间，Pod名称不能用驼峰，只能用-分隔。

查看pod的详细，可以看到podNginx这个pod，被分给了ubuntu01节点：

```bash
root@kjg-PC:~# kubectl describe pod pod-nginx
###省略
Events:
  Type    Reason     Age   From               Message
  ----    ------     ----  ----               -------
  Normal  Scheduled  13s   default-scheduler  Successfully assigned default/pod-nginx to ubuntu01
  Normal  Pulling    12s   kubelet            Pulling image "nginx"
```

验证一下，虽然是Kubernetes，但底层还是Docker在干活，可以看到Master里没有容器，而ubuntu01有：

```bash
root@kjg-PC:~# docker ps | grep nginx
root@kjg-PC:~# 
```

```bash
root@ubuntu01:~# docker ps | grep nginx
56eedd7aed3d   nginx                                               "/docker-entrypoint.…"   5 minutes ago   Up 5 minutes             k8s_pod-nginx_pod-nginx_default_680c8e7b-f3e0-4af0-a6e9-5e493d0aea00_0
26be805180f5   registry.aliyuncs.com/google_containers/pause:3.2   "/pause"                 5 minutes ago   Up 5 minutes             k8s_POD_pod-nginx_default_680c8e7b-f3e0-4af0-a6e9-5e493d0aea00_0
root@ubuntu01:~# 
```

## 配置文件新建Pod

```yaml
#指定版本
apiVersion: v1
#指定资源类型
kind: Pod
#pod的元数据
metadata:
  labels: 
  	#pod要启动哪个container，spec.containers.name相同
    run: pod-nginx
    #pod名称
  name: pod-nginx
spec:
  containers:
  	#pod包含哪些容器
  - image: nginx
  	#这个容器叫什么名字
    name: pod-nginx
```

```bash
root@kjg-PC:~# kubectl apply -f pod-nginx.yaml 
pod/pod-nginx created
```

## 删除Pod

```bash
root@kjg-PC:~# kubectl delete pod pod-nginx
pod "pod-nginx" deleted

root@kjg-PC:~# kubectl get pod -n default
No resources found in default namespace.
```

## 创建多容器Pod

这里以配置文件的方式（multi-pods.yaml）：

```yaml
#指定版本
apiVersion: v1
#指定资源类型
kind: Pod
#pod的元数据
metadata:
  labels: 
  	#pod要启动哪个container，spec.containers.name相同
    run: multi-pods
    #pod名称
  name: multi-pods
spec:
  containers:
  	#pod包含哪些容器
  - image: nginx
  	#这个容器叫什么名字
    name: pod-nginx
  - image: tomcat:8.5.68
  	name: pod-tomcat
```

```bash
root@kjg-PC:~# kubectl apply -f multi-pods.yaml 
pod/multi-pods created
root@kjg-PC:~# kubectl get pods -n default		#表示容器正在创建中。
NAME         READY   STATUS              RESTARTS   AGE
multi-pods   0/2     ContainerCreating   0          38s
root@kjg-PC:~# kubectl describe pod multi-pods	#通过describe命令，可以看到multi-pods的创建过程，它又被分到Ubuntu01了。此时正在拉取Tomcat的镜像。
Events:
  Type    Reason     Age   From               Message
  ----    ------     ----  ----               -------
  Normal  Scheduled  95s   default-scheduler  Successfully assigned default/multi-pods to ubuntu01
  Normal  Pulling    94s   kubelet            Pulling image "nginx"
  Normal  Pulled     73s   kubelet            Successfully pulled image "nginx" in 20.707299253s
  Normal  Created    73s   kubelet            Created container pod-nginx
  Normal  Started    73s   kubelet            Started container pod-nginx
  Normal  Pulling    73s   kubelet            Pulling image "tomcat:8.5.68"
root@kjg-PC:~# kubectl get pods -n default		#再等一会儿，发现multi-pods成功启动。
NAME         READY   STATUS    RESTARTS   AGE
multi-pods   2/2     Running   0          3m31s



root@ubuntu01:~# docker ps		#此时去Ubuntu01看看，multi-pods的容器确实在这运行着。
CONTAINER ID   IMAGE                                               COMMAND                  CREATED          STATUS          PORTS     NAMES
99b5a5318389   tomcat                                              "catalina.sh run"        2 minutes ago    Up 2 minutes              k8s_pod-tomcat_multi-pods_default_4c3daf92-e8dd-43a7-9d2d-dbb3f274a998_0
c7f6ae38a08d   nginx                                               "/docker-entrypoint.…"   4 minutes ago    Up 4 minutes              k8s_pod-nginx_multi-pods_default_4c3daf92-e8dd-43a7-9d2d-dbb3f274a998_0
```

```bash
root@kjg-PC:~# kubectl get pod multi-pods -owide	#找到mult-pods的详细信息，可以发现访问multi-pods的地址是172.31.3.195。
NAME         READY   STATUS    RESTARTS   AGE   IP             NODE       NOMINATED NODE   READINESS GATES
multi-pods   2/2     Running   0          43m   172.31.3.195   ubuntu01   <none>           <none>
root@kjg-PC:~# curl http://172.31.3.195			#直接curl它的80端口，访问nginx成功。
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>


root@kjg-PC:~# curl http://172.31.3.195:8080	#直接curl它的8080端口，访问tomcat成功。
<!doctype html><html lang="en"><head><title>HTTP Status 404 – Not Found</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 – Not Found</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Description</b> The origin server did not find a current representation for the target resource or is not willing to disclose that one exists.</p><hr class="line" /><h3>Apache Tomcat/8.5.68</h3></body></html>
```

## 进入某个Pod内进行操作

还是以上面的multi-pods为例，我现在要通过Kubernetes，进入里面的nginx容器（容器名是pod-nginx）、tomcat容器（容器名是pod-tomcat）进行操作：

```bash
root@kjg-PC:~# kubectl exec -it multi-pods -c pod-nginx  -- /bin/bash
root@multi-pods:/# pwd
/
root@multi-pods:/# curl
curl: try 'curl --help' or 'curl --manual' for more information
root@multi-pods:/# exit 
exit
command terminated with exit code 2
root@kjg-PC:~# kubectl exec -it multi-pods -c pod-nginx  -- /bin/bash
root@multi-pods:/# pwd
/
root@multi-pods:/# curl http://localhost:8080
<!doctype html><html lang="en"><head><title>HTTP Status 404 – Not Found</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 – Not Found</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Description</b> The origin server did not find a current representation for the target resource or is not willing to disclose that one exists.</p><hr class="line" /><h3>Apache Tomcat/8.5.68</h3></body></html> 
root@multi-pods:/# exit
exit
root@kjg-PC:~#


root@kjg-PC:~# kubectl exec -it multi-pods -c pod-tomcat  -- /bin/bash
root@multi-pods:/usr/local/tomcat# pwd
/usr/local/tomcat
root@multi-pods:/usr/local/tomcat# curl http://localhost:80
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
root@multi-pods:/usr/local/tomcat# 
```

**可以发现，Pod内的容器之间，网络是能够互相连通的。**

# Pod的封装与增强-Deployment

Deployment是Pod的封装与增强，可以控制Pod，使得Pod拥有多副本、自愈、扩容、缩容的功能。

**可以类比为：Pod是BeanFactory，Deployment是ApplicationContext。**

## 自愈

和普通的创建Pod不同，普通Pod可以人为、非人为地被删除，删除之后Pod就消失了。但由Deployment管理的Pod，即使被人**从Pod层面**删除了，Deployment也会实时监控，并重新部署Pod。

普通Pod创建Nginx、删除：

```bash
### 新建
root@kjg-PC:~# kubectl run normal-nginx --image=nginx
pod/normal-nginx created
root@kjg-PC:~# kubectl get pods -n default
NAME           READY   STATUS    RESTARTS   AGE
normal-nginx   1/1     Running   0          58s


### 删除
root@kjg-PC:~# kubectl delete pod normal-nginx
pod "normal-nginx" deleted
root@kjg-PC:~# kubectl get pods -n default
No resources found in default namespace.
```

Deployment创建Pod（**可以看到，由Deployment创建的Pod有个后缀**），从Pod层面删除：

```bash
### 新建
root@kjg-PC:~# kubectl create deployment deploy-nginx --image=nginx
deployment.apps/deploy-nginx created
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS    RESTARTS   AGE
deploy-nginx-8458f6dbbb-nzdwh   1/1     Running   0          8s

### 删除
root@kjg-PC:~# kubectl delete pod deploy-nginx-8458f6dbbb-nzdwh
pod "deploy-nginx-8458f6dbbb-nzdwh" deleted

### 发现delopy-nginx重建，后缀发生变化（ nzdwh → 2rw4x ）
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS    RESTARTS   AGE
deploy-nginx-8458f6dbbb-nzdwh   1/1     Running   0          3m56s
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS              RESTARTS   AGE
deploy-nginx-8458f6dbbb-2rw4x   0/1     ContainerCreating   0          2s
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS    RESTARTS   AGE
deploy-nginx-8458f6dbbb-2rw4x   1/1     Running   0          74s






### 我再删除一次2rw4x
root@kjg-PC:~# kubectl delete pod deploy-nginx-8458f6dbbb-2rw4x
pod "deploy-nginx-8458f6dbbb-2rw4x" deleted

### 发现deploy-nginx又重建了，后缀变成了dfb4n
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS              RESTARTS   AGE
deploy-nginx-8458f6dbbb-dfb4n   0/1     ContainerCreating   0          2s
root@kjg-PC:~# kubectl get pods -n default
NAME                            READY   STATUS    RESTARTS   AGE
deploy-nginx-8458f6dbbb-dfb4n   1/1     Running   0          44s
```

如果真的想删除deploy-nginx，只能**从Deploy层面**去删除，然后再删除Pod：

```bash
### 先删除eploy
root@kjg-PC:~# kubectl get deploy
NAME           READY   UP-TO-DATE   AVAILABLE   AGE
deploy-nginx   1/1     1            1           108m
root@kjg-PC:~# kubectl delete deploy deploy-nginx
deployment.apps "deploy-nginx" deleted
root@kjg-PC:~# kubectl get deploy
No resources found in default namespace.

### 再删除Pod
root@kjg-PC:~# kubectl get pods -A
NAMESPACE              NAME                                         READY   STATUS    RESTARTS   AGE
default                deploy-nginx-8458f6dbbb-dfb4n                1/1     Running   0          109m
kube-system            calico-kube-controllers-5bb48c55fd-r6txn     1/1     Running   9          6d5h
kube-system            calico-node-cncp8                            1/1     Running   0          3h52m
kube-system            calico-node-nl4mm                            1/1     Running   0          3h51m
kube-system            calico-node-q77lf                            1/1     Running   0          3h52m
kube-system            coredns-7f89b7bc75-b2r68                     1/1     Running   9          6d5h
kube-system            coredns-7f89b7bc75-kr2m4                     1/1     Running   9          6d5h
kube-system            etcd-kjg-pc                                  1/1     Running   10         6d5h
kube-system            kube-apiserver-kjg-pc                        1/1     Running   10         6d5h
kube-system            kube-controller-manager-kjg-pc               1/1     Running   11         6d5h
kube-system            kube-proxy-2hhr2                             1/1     Running   5          6d4h
kube-system            kube-proxy-9qdgv                             1/1     Running   9          6d5h
kube-system            kube-proxy-tkwf6                             1/1     Running   5          6d4h
kube-system            kube-scheduler-kjg-pc                        1/1     Running   12         6d5h
kubernetes-dashboard   dashboard-metrics-scraper-79c5968bdc-f88vn   1/1     Running   4          3d21h
kubernetes-dashboard   kubernetes-dashboard-658485d5c7-4d8ml        1/1     Running   3          3d4h
root@kjg-PC:~# kubectl get pods 
NAME                            READY   STATUS    RESTARTS   AGE
deploy-nginx-8458f6dbbb-dfb4n   1/1     Running   0          109m
root@kjg-PC:~# kubectl delete pod deploy-nginx-8458f6dbbb-dfb4n
pod "deploy-nginx-8458f6dbbb-dfb4n" deleted
root@kjg-PC:~# kubectl get pods 
No resources found in default namespace.
```



## 副本

使用命令行创建一个deploy-multi-nginx的部署，指定副本数为3（这里的副本可以理解为Kafka的Replica，包含所有的数量）：

```bash
root@kjg-PC:~# kubectl create deploy multi-deploy-nginx --image=nginx --replicas=3
deployment.apps/multi-deploy-nginx created
root@kjg-PC:~# kubectl get deploy	#可以看到multi-deploy-nginx的副本正在部署中（这里不知道为什么好久）
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   0/3     0            0           9s
root@kjg-PC:~# kubectl get deploy
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   3/3     3            3           4m27s
```

顺便再看一看Pod的状态，可以看到有两个在ubuntu01，一个在ubuntu02：

```bash
root@kjg-PC:~# kubectl get pods -owide
NAME                                  READY   STATUS    RESTARTS   AGE     IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          3m27s   172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Running   0          3m27s   172.31.3.200   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-phx67   1/1     Running   0          3m27s   172.31.3.201   ubuntu01   <none>           <none>



###curl也是没问题的。
root@kjg-PC:~# curl http://172.31.79.6
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>





root@kjg-PC:~# curl http://172.31.3.200
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>





root@kjg-PC:~# curl http://172.31.3.201
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

## 扩容、缩容

以上面的multi-deploy-nginx为例，原本副本数是3，现在要改为6：

```bash
root@kjg-PC:~# kubectl scale deploy/multi-deploy-nginx --replicas=6
deployment.apps/multi-deploy-nginx scaled
root@kjg-PC:~# kubectl get deploy
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   3/6     6            3           31m
root@kjg-PC:~# kubectl get deploy
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   6/6     6            6           33m
```

再看一下每一个Pod，具体在哪里：

```bash
root@kjg-PC:~# kubectl get pods -n default -owide
NAME                                  READY   STATUS    RESTARTS   AGE     IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          31m     172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Running   0          31m     172.31.3.200   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-htrc6   1/1     Running   0          2m27s   172.31.3.202   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-mdmkd   1/1     Running   0          2m27s   172.31.79.7    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-phx67   1/1     Running   0          31m     172.31.3.201   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-vx2dn   1/1     Running   0          2m27s   172.31.79.8    ubuntu02   <none>           <none>
```

每个都访问试了一下，没有问题。

现在我又嫌太多了，想把副本数改为4：

```bash
root@kjg-PC:~# kubectl scale deploy/multi-deploy-nginx --replicas=4
deployment.apps/multi-deploy-nginx scaled
root@kjg-PC:~# kubectl get deploy
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   4/4     4            4           35m
```

看得出来，缩容很快，再看看每个Pod的位置：

```bash
NAME                                  READY   STATUS    RESTARTS   AGE     IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          33m     172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Running   0          33m     172.31.3.200   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-htrc6   1/1     Running   0          4m36s   172.31.3.202   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-phx67   1/1     Running   0          33m     172.31.3.201   ubuntu01   <none>           <none>
```

## 故障转移

还是以上满的multi-deploy-nginx为例，为了方便观察，将副本数改为2：

```bash
root@kjg-PC:~# kubectl scale deploy/multi-deploy-nginx --replicas=2
deployment.apps/multi-deploy-nginx scaled
root@kjg-PC:~# kubectl get pods -n default -owide
NAME                                  READY   STATUS    RESTARTS   AGE   IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          37m   172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Running   0          37m   172.31.3.200   ubuntu01   <none>           <none>
```

我现在把ubuntu01关机，也就是5f8lg这个Pod会在Deployment失联。Kubernetes已经感知到其中一个Pod不可用了，**但要等5分钟，Kubernetes才能故障转移**：

```bash
root@kjg-PC:~# kubectl get deploy -n default && echo
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   1/2     2            1           48m

root@kjg-PC:~# kubectl get pods -n default -owide
NAME                                  READY   STATUS    RESTARTS   AGE   IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          48m   172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Running   0          48m   172.31.3.200   ubuntu01   <none>           <none>


### 5分钟后。



root@kjg-PC:~# kubectl get deploy -n default && echo
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
multi-deploy-nginx   2/2     2            2           54m
root@kjg-PC:~# kubectl get pods -n default -owide
NAME                                  READY   STATUS        RESTARTS   AGE   IP             NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running       0          51m   172.31.79.6    ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-5f8lg   1/1     Terminating   0          51m   172.31.3.200   ubuntu01   <none>           <none>
multi-deploy-nginx-785f995c7d-qnb5n   1/1     Running       0          37s   172.31.79.9    ubuntu02   <none>           <none>
```

可以发现，5f8lg因为ubuntu01的宕机而被终止，Kubernetes新建了一个qnb5n的Pod，转移到ubuntu02上。

即使后面ubuntu01恢复了，5f8lg也不会重新运行在ubuntu01，而是直接在pod记录里消失：

```bash
root@kjg-PC:~# kubectl get nodes
NAME       STATUS   ROLES                  AGE    VERSION
kjg-pc     Ready    control-plane,master   6d6h   v1.20.9
ubuntu01   Ready    <none>                 6d6h   v1.20.9
ubuntu02   Ready    <none>                 6d6h   v1.20.9
root@kjg-PC:~# kubectl get pods -n default -owide
NAME                                  READY   STATUS    RESTARTS   AGE    IP            NODE       NOMINATED NODE   READINESS GATES
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running   0          59m    172.31.79.6   ubuntu02   <none>           <none>
multi-deploy-nginx-785f995c7d-qnb5n   1/1     Running   0          8m4s   172.31.79.9   ubuntu02   <none>           <none>
```



## 滚动更新

### 过程

假设Kubernetes已经跑着一个Deployment，这个Deployment的Pod1有3个副本，均匀地分布在Master和Node上。通过Nginx挡在最外层，接收外部请求，转发到Kubernetes集群：

![04](03-Kubernetes的进阶概念.assets/04.png)

现在Pod1这个应用进行了版本迭代，应用新版本 打包成 **镜像新版本** 发布到镜像仓库里。现在需要Deployment升级Pod1到新版本，使用滚动更新的话是这样的：

**核心过程是：先将某个节点的流量访问关闭，等待该节点的Pod1更新为Version2后，再将流量恢复。**

![05](03-Kubernetes的进阶概念.assets/05.png)

值得注意的是，在某个节点恢复流量后，本质对外暴露的是新版本应用。如果此时其他节点还未更新，有可能会导致 **用户两次请求的应用版本不一致**。这本质和灰度发布的数据一致性问题一样，**我相信这有成熟的解决方案，只是目前我还不清楚**。

### 实践

可以通过kubectl，直接修改 deployment的 pod的 nginx容器的镜像，从而实现滚动更新的过程：

```bash
root@kjg-PC:~# kubectl set image deploy/multi-deploy-nginx nginx=nginx:1.16.1  --record
deployment.apps/multi-deploy-nginx image updated
root@kjg-PC:~# kubectl get pods -n default
NAME                                  READY   STATUS              RESTARTS   AGE
multi-deploy-nginx-785f995c7d-2qn8l   1/1     Running             0          100m
multi-deploy-nginx-785f995c7d-qnb5n   1/1     Running             0          49m
multi-deploy-nginx-7d859575f5-d9cgq   0/1     ContainerCreating   0          21s
root@kjg-PC:~# kubectl get pods -n default
NAME                                  READY   STATUS    RESTARTS   AGE
multi-deploy-nginx-7d859575f5-d9cgq   1/1     Running   0          52s
multi-deploy-nginx-7d859575f5-hggs4   1/1     Running   0          17s
```

可以看到 d9cqg和hggs4正在逐渐替代2qn8l和qnb5n，只有新Pod启动后，才会将旧Pod给Kill掉。

## 版本回退

刚才将 deployment的 pod的 nginx容器的镜像改为1.16.1版本，现在想回滚到上一版，要怎么做呢？

先查看multi-deploy-nginx的历史记录：

```bash
root@kjg-PC:~# kubectl rollout history deploy/multi-deploy-nginx
deployment.apps/multi-deploy-nginx 
REVISION  CHANGE-CAUSE
1         <none>
2         kubectl set image deploy/multi-deploy-nginx nginx=nginx:1.16.1 --record=true
```

其中REVISION1是上一个版本，REVISION2是最新版本，有了REVISION号，就能够回滚：

```bash
root@kjg-PC:~# kubectl rollout undo deploy/multi-deploy-nginx --to-revision=1
deployment.apps/multi-deploy-nginx rolled back
root@kjg-PC:~# kubectl get pods -n default
NAME                                  READY   STATUS              RESTARTS   AGE
multi-deploy-nginx-785f995c7d-lxpps   0/1     ContainerCreating   0          6s
multi-deploy-nginx-7d859575f5-d9cgq   1/1     Running             0          8m13s
multi-deploy-nginx-7d859575f5-hggs4   1/1     Running             0          7m38s
root@kjg-PC:~# kubectl get pods -n default
NAME                                  READY   STATUS    RESTARTS   AGE
multi-deploy-nginx-785f995c7d-lxpps   1/1     Running   0          31s
multi-deploy-nginx-785f995c7d-m4bxj   1/1     Running   0          12s
```

和滚动更新一样，版本回退也是新创建pod，创建成功后再替代。
