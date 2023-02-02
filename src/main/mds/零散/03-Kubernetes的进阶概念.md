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


