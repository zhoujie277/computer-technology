#!/bin/sh

# if 语句和 条件测试语句 [ xxx ]的使用

if [ -d test_test ]; then
    echo "It 's a dir"
elif [ -f test_test ]; then
    echo "It's a file"
else
    printf "hello world\n"
fi
