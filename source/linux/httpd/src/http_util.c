#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#include "../include/http_util.h"
#include "../include/error_util.h"

// 通过文件名获取文件的类型
const char *get_file_type(const char *name)
{
    char *dot;

    // 自右向左查找‘.’字符, 如不存在返回NULL
    dot = strrchr(name, '.');
    if (dot == NULL)
        return "text/plain; charset=utf-8";
    if (strcmp(dot, ".html") == 0 || strcmp(dot, ".htm") == 0)
        return "text/html; charset=utf-8";
    if (strcmp(dot, ".jpg") == 0 || strcmp(dot, ".jpeg") == 0)
        return "image/jpeg";
    if (strcmp(dot, ".gif") == 0)
        return "image/gif";
    if (strcmp(dot, ".png") == 0)
        return "image/png";
    if (strcmp(dot, ".css") == 0)
        return "text/css";
    if (strcmp(dot, ".au") == 0)
        return "audio/basic";
    if (strcmp(dot, ".wav") == 0)
        return "audio/wav";
    if (strcmp(dot, ".avi") == 0)
        return "video/x-msvideo";
    if (strcmp(dot, ".mov") == 0 || strcmp(dot, ".qt") == 0)
        return "video/quicktime";
    if (strcmp(dot, ".mpeg") == 0 || strcmp(dot, ".mpe") == 0)
        return "video/mpeg";
    if (strcmp(dot, ".vrml") == 0 || strcmp(dot, ".wrl") == 0)
        return "model/vrml";
    if (strcmp(dot, ".midi") == 0 || strcmp(dot, ".mid") == 0)
        return "audio/midi";
    if (strcmp(dot, ".mp3") == 0)
        return "audio/mpeg";
    if (strcmp(dot, ".ogg") == 0)
        return "application/ogg";
    if (strcmp(dot, ".pac") == 0)
        return "application/x-ns-proxy-autoconfig";

    return "text/plain; charset=utf-8";
}

void write_response_header(int no, char *msg, const char *type, char *out, long len)
{
    // 状态行
    sprintf(out + strlen(out), "HTTP/1.1 %d %s\r\n", no, msg);
    // 消息报头
    sprintf(out + strlen(out), "Content-Type:%s\r\n", type);
    sprintf(out + strlen(out), "Content-Length:%ld\r\n", len);
    // 空行
    sprintf(out + strlen(out), "\r\n");
}

void handle_error_page(char *buf, int status, char *title, char *text)
{
    sprintf(buf, "%s %d %s\r\n", "HTTP/1.1", status, title);
    sprintf(buf + strlen(buf), "Content-Type:%s\r\n", "text/html");
    sprintf(buf + strlen(buf), "Content-Length:%d\r\n", -1);
    sprintf(buf + strlen(buf), "Connection: close\r\n");
    sprintf(buf + strlen(buf), "\r\n");
    sprintf(buf + strlen(buf), "<html><head><title>%d %s</title></head>\n", status, title);
    sprintf(buf + strlen(buf), "<body bgcolor=\"#cc99cc\"><h2 align=\"center\">%d %s</h4>\n", status, title);
    sprintf(buf + strlen(buf), "%s\n", text);
    sprintf(buf + strlen(buf), "<hr>\n</body>\n</html>\n");
}

void handle_opendir(const char *dirname, char *writebuf)
{
    sprintf(writebuf + strlen(writebuf), "<html><head><title>目录名: %s</title></head>", dirname);
    sprintf(writebuf + strlen(writebuf), "<body><h1>当前目录: %s</h1><table>", dirname);

    struct dirent **ptr;
    int num = scandir(dirname, &ptr, NULL, alphasort);

    char path[64] = {0};
    for (int i = 0; i < num; i++)
    {
        char *name = ptr[i]->d_name;
        sprintf(path, "%s/%s", dirname, name);
        struct stat st;
        stat(path, &st);

        if (S_ISREG(st.st_mode))
        {
            sprintf(writebuf + strlen(writebuf),
                    "<tr><td><a href=\"%s\">%s</a></td><td>%ld</td></tr>",
                    name, name, (long)st.st_size);
        }
        else if (S_ISDIR(st.st_mode))
        {
            sprintf(writebuf + strlen(writebuf),
                    "<tr><td><a href=\"%s/\">%s/</a></td><td>%ld</td></tr>",
                    name, name, (long)st.st_size);
        }
    }
}

void write_file(const char *path, char *out)
{
    int fd = open(path, O_RDONLY);
    if (fd == -1)
        perror("open error");
    int ret;
    char buf[BUFSIZ];
    while ((ret = read(fd, buf, BUFSIZ)) > 0)
    {
        strcat(out, buf);
    }
}

void handleRequest(const char *path, char *out, size_t *out_size)
{
    const char *file;
    if (strcmp(path, "/") == 0)
    {
        file = "./";
    }
    else
    {
        file = path + 1;
    }
    printf("would access file %s \n", file);
    struct stat ss;
    int ret = stat(file, &ss);
    if (ret == -1)
    {
        handle_error_page(out, 404, "Not Found", "No such file or directory");
        perror("file stat");
        return;
    }

    bzero(out, strlen(out));
    if (S_ISDIR(ss.st_mode))
    {
        write_response_header(200, "OK", get_file_type(".html"), out, -1);
        handle_opendir(file, out);
    }
    // 普通文件
    else if (S_ISREG(ss.st_mode))
    {
        write_response_header(200, "OK", get_file_type(file), out, ss.st_size);
        write_file(file, out);
    }
    *out_size = strlen(out);
    // printf("write callback %ld \n", *out_size);
    // printf("%s", out);
}

void handleHttp(char *in, char *out, size_t *out_size)
{
    if (strncasecmp(in, "GET", 3) == 0)
    {
        char method[12], path[1024], protocol[12];
        char *p = strtok(in, "\r\n");
        sscanf(p, "%[^ ] %[^ ] %[^ ]", method, path, protocol);
        printf("method = %s, path = %s, protocol = %s \n", method, path, protocol);
        handleRequest(path, out, out_size);
    }
    else
    {
        printf("###### = %s", in);
    }
}
