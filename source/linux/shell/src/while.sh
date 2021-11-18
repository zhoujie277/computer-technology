#!/bin/sh

# while 语句演示; 输入密码，成功正常结束；大于3次没输入正确则退出。

count=2

echo "Enter password:"
read TRY
while [ "$TRY" != "secret" -a "$count" -gt 0 ]; do 
    echo "sorry, try again"
    count=$[count-1]
    read TRY
done
