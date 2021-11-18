
struct hello
{
    int data;
    char place;
};

struct hello g;

void test()
{
    g.place = 'a';
    // static struct hello kk;
    // static int a = 10;
    // char buf[256];
    // int b = sizeof(buf);
    int b = g.data;
    int c = g.data;
    int d = b;
    // int d = kk.data;
}