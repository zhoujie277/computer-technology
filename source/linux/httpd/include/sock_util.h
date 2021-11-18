#ifndef MACRO_SOCK_UTIL

#define MACRO_SOCK_UTIL

typedef void (*handle_line)(char *in, int in_size, char *out, size_t *out_size);

void start_server(int port, const handle_line on_handle_line);

#endif
