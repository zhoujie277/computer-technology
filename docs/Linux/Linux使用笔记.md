# Linux 使用笔记

### VMWare 使用笔记

虚拟机挂载宿主共享文件夹命令：
```
sudo /usr/bin/vmhgfs-fuse .host:/ /mnt/hgfs -o allow_other -o uid=1000 -o gid=1000 -o umask=022
```

上面命令中的 uid 和 gid 需要根据 ubuntu 用户 uid 来决定，在home目录输入id 命令即可查询
```
    id
```

卸载目录
```
    sudo umount /mnt/hgfs
```

重启发现权限又变回去，需要设置开机自动挂载
```
    sudo vim /etc/fstab //打开配置文件
```

设置开机自动挂载
```
.host:/ /mnt/hgfs fuse.vmhgfs-fuse allow_other,uid=1000,gid=1000,umask=022  0  0
```