#!/bin/sh

# 函数功能演示，shell中函数没有返回值也没有参数列表
# 函数传参演示

foo() {
    echo "Function foo is called";
    echo $0
    echo $1
    echo $2
    echo $3
    echo $@
    echo $#
    echo "Function foo end..."
}
echo "startproc..."
foo $0 $1 $2 $3
echo "endproc..."

