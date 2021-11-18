#!/bin/sh


count=1
sum=0
while [ $count -le 100 ]; do
    sum=$[$count+$sum]
    count=$[$count+1]
done
echo "sum is $sum"
