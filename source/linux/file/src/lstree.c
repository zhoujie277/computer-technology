#include <stdio.h>
#include <dirent.h>
#include <sys/stat.h>

#define MAX_DEPTH 3

void ls(const char *path, int depth);

void lsdir(const char *name, int depth)
{
    DIR *dir = opendir(name);
    if (dir == NULL)
    {
        perror("opendir was error");
        return;
    }
    char path[256];
    // dirent 目录项
    struct dirent *dt;
    while ((dt = readdir(dir)) != NULL)
    {
        if (dt->d_name[0] == '.')
            continue;
        sprintf(path, "%s/%s", name, dt->d_name);
        ls(path, depth + 1);
    }

    closedir(dir);
}

void ls(const char *path, int depth)
{
    if (depth == MAX_DEPTH)
        return;
    struct stat pathstat;
    int ret = lstat(path, &pathstat);
    if (ret == -1)
    {
        perror("stt error");
        return;
    }
    for (int i = 0; i < depth; i++)
    {
        if (i == depth - 1)
        {
            printf("--");
        }
        else
        {
            printf("   ");
        }
    }
    if (S_ISDIR(pathstat.st_mode))
    {
        printf("%s\t\n", path);
        lsdir(path, depth);
    }
    else
    {
        printf("%s\t%lld\n", path, pathstat.st_size);
    }
}

int main(int argc, char *argv[])
{
    if (argc == 1)
    {
        ls(".", 0);
    }
    else
    {
        ls(argv[1], 0);
    }
    return 0;
}