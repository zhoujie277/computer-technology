#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <string.h>

/**
 * 内存映射示例
 * 创建 mmap 至少需要读权限，写权限 ≤ 文件打开的写权限。
 */
int main()
{
    int fd = open("./resource/mmap", O_RDWR | O_CREAT | O_TRUNC);
    if (fd == -1)
    {
        perror("open error");
        return 1;
    }
    // 如下方法和 ftruncate 都可以扩展文件大小
    // int ret = lseek(fd, 10, SEEK_END);
    // write(fd, "\0", 1);

    ftruncate(fd, 10);
    int len = lseek(fd, 0, SEEK_END);
    printf("the mmap filesize is %d \n ", len);
    char *p = mmap(NULL, len, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (p == MAP_FAILED)
    {
        perror("mmap error");
        return 1;
    }
    strcpy(p, "Hello mmap");

    printf("----%s \n ", p);

    int ret = munmap(p, len);
    if (ret == -1)
    {
        perror("munmap error");
        return 2;
    }
    return 0;
}