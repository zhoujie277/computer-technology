# Git 指南

## 一、快速入门

### Git 文件状态
---- | 状态 | 动作 | 描述
---- | --- | ---- | ----
Modifyed | 已修改 | Edit file | 在工作区修改文件
Staged   | 已暂存 | git add filename | 对已修改的文件执行 Git 暂存操作，将文件存入暂存区。
Commited | 已提交 | git commit -m message | 将已暂存的文件执行 Git 提交操作，将文件存入版本库。

#### Git 配置文件
路径    |  命令  |   描述
------ | ------ | ------ |
/etc/gitconfig | git config --system | 
~/.gitconfig | git config --global | 查看/修改当前用户的配置属性
.git/config | git config --local   | 查看/修改当前工程的配置属性