src = ${wildcard src/*.c}
target = $(patsubst src/%.c, build/%, $(src))

ALL:$(target)

https_client:src/https_client.c
	gcc src/https_client.c -o build/https_client -L/usr/local/opt/openssl@1.1/lib -I/usr/local/opt/openssl@1.1/include

build/%:src/%.c
	gcc $< -o $@

server:src/server.c
	gcc $< -o build/$@ -pthread

%:src/%.c
	gcc $< -o build/$@  

clean:				
	-rm -rf $(target)
