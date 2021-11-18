#!/bin/sh

# for 语句演示

#for FRUIT in apple banana pear; do
#    echo "I like $FRUIT"
#done

for filename in $(ls) ; do
    echo $filename
    if [ -d "$filename" ]; then
        echo "it is a dir."
    elif [ -f "$filename" ]; then
        echo "it is a file."
    else 
        echo "other."
    fi
done
