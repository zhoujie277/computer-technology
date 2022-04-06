all: main_s main_d

# 编译可执行文件: 将 main.c 和 part.c 一起编译成 main_s
main_s: main.o part.o
	gcc main.o part.o -o main_s

# 编译可执行文件，将 main.o 和库 libpart.so 动态连接到 main_d
main_d: main.o libpart.so
	gcc main.o -L. -lpart -o main_d

# 编译动态链接库 libpart.so 文件
libpart.so: part.o
	gcc --shared part.o -o libpart.so

# 编译成中间文件 main.o
main.o: main.c
	gcc -c main.c -o main.o

# 编译成中间文件 part.o
part.o: part.c 
	gcc -c part.c -o part.o

clean:
	rm -f *.o *.so main_s main_d
