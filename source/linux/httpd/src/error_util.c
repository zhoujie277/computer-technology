
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

void check_error(int ret, const char *str)
{
    if (ret >= 0) return;
    if (errno == EAGAIN || errno == EWOULDBLOCK) return;
    char buf[1024];
    sprintf(buf, "%s ret= %d", str, ret);
    perror(buf);
    exit(EXIT_FAILURE);
}
