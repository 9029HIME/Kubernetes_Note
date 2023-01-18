# 机器环境准备

Kubernetes对运行环境有点要求，在安装Kubernetes之前，要做以下准备：

1. 关闭SELinux

   ```bash
   sudo setenforce 0
   sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config
   ```

2. 关闭swap

   ```bash
   swapoff -a  
   sed -ri 's/.*swap.*/#&/' /etc/fstab
   ```

3. 允许 iptables 检查桥接流量

   ```bash
   cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
   br_netfilter
   EOF
   
   cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
   net.bridge.bridge-nf-call-ip6tables = 1
   net.bridge.bridge-nf-call-iptables = 1
   EOF
   sudo sysctl --system
   ```

以上3步准备完毕后，就能够安装Kubernetes了。

# 安装流程预演

Kubernetes和其他分布式组件不太一样，它比较复杂，有必要提前说一下 安装步骤 和 安装的内容 分别是干嘛的。

1. 初始环境，机子需要有容器运行环境，这里以Docker为例：

   ![01](02-Kubernetes的搭建.assets/01.png)

2. 安装kubelet，将Kubernetes的基础功能先细分到 **每台机子** 上：

   ![02](02-Kubernetes的搭建.assets/02.png)

3. 安装kubectl和kubeadm，kubectl帮助我们 **操作** Kubernetes，kubeadm帮助我们 **管理** 和 下一步安装Kubernetes：

   ![03](02-Kubernetes的搭建.assets/03.png)

4. 在Master机器 调用 kubeadm init命令，将Master机初始化为Kubernetes的主节点，生成主节点专属的组件，如api-server、scheduler、kube-proxy、etcd、controller-manage：

   ![04](02-Kubernetes的搭建.assets/04.png)

5. 在其他Node机器 调用 kubeadm join命令，加入Master节点，成为Kubernetes集群的一部分，生成kube-proxy：

   ![05](02-Kubernetes的搭建.assets/05.png)

6. 自此，一个Kubernetes集群搭建完毕。

