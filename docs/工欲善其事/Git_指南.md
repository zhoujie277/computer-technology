# Git 指南

## 一、快速入门

### Git 文件状态
--   | 状态 | 动作 | 描述
---- | --- | ---- | ----
Modifyed | 已修改 | Edit file | 在工作区修改文件
Staged   | 已暂存 | git add filename | 对已修改的文件执行 Git 暂存操作，将文件存入暂存区。
Commited | 已提交 | git commit -m message | 将已暂存的文件执行 Git 提交操作，将文件存入版本库。

#### 状态转换与相关命令
状态转换与回退   |    相关命令
-------------- | ----
工作区 -> 暂存区 | git add .
暂存区 -> 版本库 | git commit 
版本库 -> 暂存区 | git reset HEAD
暂存区 -> 工作区 | git checkout .

#### Git 配置文件
路径    |  命令  |   描述
------ | ------ | ------ |
/etc/gitconfig | git config --system | 查看/修改本机的配置属性
~/.gitconfig | git config --global | 查看/修改当前用户的配置属性
.git/config | git config --local   | 查看/修改当前工程的配置属性


#### Git 忽略文件
```
    touch .gitigore

    *.b                 忽略所有以 b 为结尾的文件
    !a.b                但a.b除外，a.b 将会添加到版本管理当中
    /TODO               忽略根目录下的 TODO 文件，不包括 subbdir/TODO
    /*/TODO             忽略根目录下子目录下的 TODO 文件
    build/              忽略 build/ 目录下的所有文件
    doc/*.txt           忽略 doc 目录下所有的 txt 文件，不包括 doc的子目录
    doc/*/*.txt         忽略 doc 目录下子目录的 txt 文件
    doc/**/*.txt        忽略 doc 下所有子孙目录的 txt 文件
```

### Git 分支

命令 | 动作
---- | ----
git branch | 显示分支
git checkout branchName | 切换分支