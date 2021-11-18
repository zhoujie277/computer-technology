#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include "../include/sock_util.h"
#include "../include/http_util.h"
#include "../include/error_util.h"

void onHandleLine(char *in, int in_size, char *out, size_t *out_size)
{
    handleHttp(in, out, out_size);
}

int main(int argc, char *argv[])
{
    if (argc < 3)
    {
        printf("please input ./server path port\n");
        exit(EXIT_FAILURE);
    }
    int ret = chdir(argv[1]);  // 改变当前工作目录
    check_error(ret, "chdir error");
    int port = atoi(argv[2]);
    start_server(port, onHandleLine);
    return EXIT_SUCCESS;
}