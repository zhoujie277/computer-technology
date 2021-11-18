src = ${wildcard src/*.c}
target := build/httpd
CC := gcc
RM := -rm -rf

ALL:$(src)
	$(CC) $^ -o $(target) 

clean:				
	$(RM) $(target)
