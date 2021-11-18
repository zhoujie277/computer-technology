src = ${wildcard src/*.c}
target = $(patsubst src/%.c, build/%, $(src))

ALL:$(target)

build/%:src/%.c
	gcc $< -o $@

server:src/server.c
	gcc $< -o build/$@ -pthread

%:src/%.c
	gcc $< -o build/$@  

clean:				
	-rm -rf $(target)
