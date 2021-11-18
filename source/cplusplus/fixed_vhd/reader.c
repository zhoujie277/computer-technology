#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/stat.h>

#define VHD_FILE "/Users/jayzhou/VirtualBoxVMs/JayOS.vhd"
#define VHD_LAST_OFFSET 512
#define VHD_CYLINDER_OFFSET 56

/**
 * 该程序读固定虚拟硬盘VHD的磁道扇区等信息。
 * 目前目录暂时固定硬编码为 VHD_FILE
 * 关于VHD文件规范请参考：VHD说明书中有关 Hard DiskFooterFormat 一节。
 * 
 * @author 
 */
void readFile() 
{
    int fd = open(VHD_FILE, O_RDONLY);
    if (fd == -1) 
    {
        perror("open failed");
        return;
    }
    int ret = 0;

    struct stat st;
    ret = stat(VHD_FILE, &st);
    if (ret == -1) 
    {
        perror("stat error");
        return;
    }
    printf("The vhd file stat:\n");
    printf("ID of device containing file: %d\n", st.st_dev);
    printf("inode number: %llu\n", st.st_ino);
    printf("file type & mode : %d\n", st.st_mode);
    printf("number of hard links: %d\n", st.st_nlink);
    printf("user ID of owner: %d\n", st.st_uid);
    printf("group ID of owner: %d\n", st.st_gid);
    printf("device ID (if special file): %d\n", st.st_rdev);
    printf("total size, in bytes: %lld\n", st.st_size);
    printf("blocksize for file system I/O: %d\n", st.st_blksize);
    printf("number of 512B blocks allocated: %lld\n", st.st_blocks);

    printf("\n");
    ret = lseek(fd, -VHD_LAST_OFFSET, SEEK_END);
    printf("lseek ret is %d \n", ret);
    if (ret == -1)
    {
        perror("lseek error");
        return;
    }
    char buf[VHD_LAST_OFFSET];
    ret = read(fd, buf, VHD_LAST_OFFSET);
    if (ret <= 0)
    {
        perror("read error");
        return;
    }
    printf("read file is: %d\n", ret);

    // 硬盘参数占4字节。此字段存储硬盘的磁道，磁头和每磁道的扇区值
    // Cylinder 2个字节
    int cylinder = (buf[VHD_CYLINDER_OFFSET] | (buf[VHD_CYLINDER_OFFSET + 1] << 8)) & 0x0000FFFF;
    // Heads 1个字节
    int heads = buf[VHD_CYLINDER_OFFSET + 2] & 0x000000FF;
    // Sectors per track/cylinder 1个字节
    int sectors = buf[VHD_CYLINDER_OFFSET + 3] & 0x000000FF;
    printf("VHD File information: \n");
    printf("磁道/柱面 cylinder: %d \n", cylinder);
    printf("磁头 heads: %u \n", heads);
    printf("每磁道的扇区数 sectors: %d \n", sectors);

    ret = close(fd);
    if (ret == -1)
    {
        perror("close error");
        return;
    }
}

int main(int argc, char** argv) 
{
    readFile();
    return 0;
}