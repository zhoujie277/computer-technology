src = $(wildcard src/*.c)	#add.c sub.c entry.c
obj = $(patsubst src/%.c, obj/%.o, $(src))	# add.o sub.o entry.o

ALL:build/a.out

build/a.out: $(obj)
	gcc $^ -o $@

# 模式规则
$(obj):obj/%.o:src/%.c
	gcc -c $< -o $@

clean:				# rm 命令前"-" 的作用是删除不存在的文件时，不报错
	-rm -rf $(obj) build/a.out

######
# src = $(wildcard *.c)	#add.c sub.c entry.c
# obj = $(patsubst %.c, %.o, $(src))	# add.o sub.o entry.o

# ALL:a.out

# a.out: $(obj)
# 	gcc $^ -o $@

# clean:				# rm 命令前"-" 的作用是删除不存在的文件时，不报错
# 	-rm -rf $(obj) a.out

# # 应用模式规则
# %.o:%.c
# 	gcc -c $< -o $@
######


# add.o:add.c
# 	gcc -c $< -o $@

# sub.o:sub.c
# 	gcc -c $< -o $@

# entry.o:entry.c
# 	gcc -c $< -o $@

# ALL:a.out

# add.o:add.c
# 	gcc -c add.c -o add.o

# sub.o:sub.c
# 	gcc -c sub.c -o sub.o

# a.out:entry.c add.o sub.o
# 	gcc entry.c add.o sub.o -o a.out