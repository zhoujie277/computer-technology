#include <string.h>

int startWith(const char *src, const char *dst)
{
    int slen = strlen(src);
    int dlen = strlen(dst);
    return slen < dlen ? 0 : memcmp(src, dst, dlen) == 0;
}