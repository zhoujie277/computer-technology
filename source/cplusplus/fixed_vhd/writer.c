#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

#define MBR_FILE "/Users/jayzhou/work/owner/code/github/computer-technology/source/assembly/13_kernel_mbr.bin"
#define KERNEL_FILE "/Users/jayzhou/work/owner/code/github/computer-technology/source/assembly/13_kernel.bin"
#define USER_FILE "/Users/jayzhou/work/owner/code/github/computer-technology/source/assembly/13_kernel_user.bin"
#define VHD_FILE "/Users/jayzhou/VirtualBoxVMs/JayOS.vhd"
#define SECTOR_SIZE 512

/**
 * 将指定文件的编译后的机器码写入到虚拟磁盘的0号扇区内。
 */
void write_mbr()
{
    int infd = open(MBR_FILE, O_RDONLY);
    if (infd == -1) 
    {
        perror("open failed");
        return;
    }
    char buf[SECTOR_SIZE];
    int ret = read(infd, buf, SECTOR_SIZE); 
    if (ret == -1)
    {
        perror("read failed");
        return;
    }

    ret = close(infd);
    if (ret == -1) 
    {
        perror("close failed");
        return;
    }

    int outfd = open(VHD_FILE, O_WRONLY);
    if (outfd == -1)
    {
        perror("open vhd file failed");
        return;
    }

    ret = write(outfd, buf, SECTOR_SIZE);
    if (ret == -1)
    {
        perror("write failed");
        return;
    }
    ret = close(outfd);
    if (ret == -1)
    {
        perror("close vhd fd error");
        return;
    }
    printf("write mbr success!\n");
}

/**
 * 将指定文件的编译后的机器码写入到虚拟磁盘的指定逻辑扇区内
 * LBA
 */
void write_lba(const char* file, int lba)
{
    int infd = open(file, O_RDONLY);
    if (infd == -1) 
    {
        perror("open failed");
        return;
    }
    int outfd = open(VHD_FILE, O_RDWR);
    if (outfd == -1)
    {
        perror("open vhd file failed");
        return;
    }

    int ret = 0;

    int offset = lba * SECTOR_SIZE;
    ret = lseek(outfd, offset, SEEK_SET);
    if (ret == -1)
    {
        perror("lseek failed");
        return;
    }

    char buf[SECTOR_SIZE];
    while((ret = read(infd, buf, SECTOR_SIZE)) > 0)
    {
        ret = write(outfd, buf, SECTOR_SIZE);
        if (ret == -1)
        {
            perror("write failed");
            return;
        }
    }
    if (ret == -1)
    {
        perror("read failed");
        return;
    }
    ret = close(infd);
    if (ret == -1) 
    {
        perror("close failed");
        return;
    }

    ret = close(outfd);
    if (ret == -1)
    {
        perror("close vhd fd error");
        return;
    }
    printf("write user success!\n");
}

int main(int argc, char** argv) 
{
    write_mbr();
    write_lba(KERNEL_FILE, 1);
    write_lba(USER_FILE, 50);
    return 0;
}