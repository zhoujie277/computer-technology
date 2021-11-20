# Git 指南

## 一、快速入门

### 1. 文件状态
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

### 2. 配置文件
路径    |  命令  |   描述
------ | ------ | ------ |
/etc/gitconfig | git config --system | 查看/修改本机的配置属性
~/.gitconfig | git config --global | 查看/修改当前用户的配置属性
.git/config | git config --local   | 查看/修改当前工程的配置属性


### 3. 忽略文件
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

### 4. 查看提交
```
    查看提交日志
    git log                     // 查看提交列表
    git log -2                  // 查看最近的 n 条提交
    git log --groph         // 图形化方式查看详细提交列表(有追踪线)
    git log --abbrev-commit     // 以缩写形式查看
    git log --pretty=oneline     // 以一行形式查看

    查看操作日志：
    git reflog      // 可查看每一次操作的日志。可用于回退到任意版本。

```

## 二、Git 使用

### 1. Git 分支

命令 | 动作
---- | ----
git branch | 显示分支
git checkout bName | 切换分支
git checkout -b bName | 新建 bName 分支
git branch -d/D bName | 删除 bName 分支(强制)


### 2. 分支合并

##### Commited 链

##### Fast-Forward
```
    (master:) git merge dev
```
+ 如果可能，合并分支时 Git 会使用 Fast-Forward 模式
+ 一般在当前分支落后于待合并的分支的commitId时且当前分支无新的 commit 会被使用，
+ 该模式不会新增一条提交。
+ Fast-Forward 合并后，该分支的指针直接指向 dev 最新的commit。
+ 可用参数禁用 Fast-Forward 模式，强制新增一条提交。如
```
    git merge --no-ff dev 
```

### 3. 版本回退

命令|描述
----|----
git reset -hard HEAD^    | 回退到上个版本
git reset -hard HEAD^^   | 回到上上个版本
git reset -hard commitid | 回到指定 commitId