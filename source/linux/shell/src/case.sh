#!/bin/sh

# case 语句的使用,  ;;表示break跳出

echo "Is it morning? Please answer yes or no."
read YES_OR_NO
case "$YES_OR_NO" in
    yes|y|Yes|YES) # 是否与其中一个值匹配
        echo "Good Morning!";;
    [nN]*)  # 表示n开头的字符串
        echo "Good Afternoon!";;
    *) # 其它情况
        echo "Sorry, $YES_OR_NO not recongnized.Enter yes or no.";;
esac
