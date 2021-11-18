
union data
{
    short k;
    // short int l;
    // int i;
    // long j;
    // void *p;
};


int hello()
{
    short a = 10;
    union data d;
    d.k = a;
    // d.p = &a;
    return 0;
}