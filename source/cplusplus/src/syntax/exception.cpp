#include <iostream>
using namespace std;

// void outOfMemory()
// {
//     try
//     {
//         int *p = new int[100000000];
//     }
//     catch (...)
//     {
//     }
// }

class Exception
{
public:
    virtual const char *what() const = 0;
    virtual int code() const = 0;
};

class DivideException : public Exception
{
public:
    const char *what() const
    {
        return "不能除以0";
    }
    int code() const
    {
        return 202;
    }
};

int divide(int x, int y)
{
    if (y == 0)
        throw DivideException();
    return x / y;
}

int devide(int x, int y)
{
    if (y == 0)
        throw "by zero";
    return x / y;
}

void check()
{
    int a = 10;
    int b = 0;
    try
    {
        int c = devide(a, b);
        cout << c << endl;
    }
    catch (const char *e)
    {
        std::cerr << e << '\n';
    }
}

int main()
{
    check();
    int a = 10;
    int b = 0;
    try
    {
        int c = divide(a, b);
    }
    catch (const Exception &exception)
    {
        cout << exception.what() << endl;
    }
    catch(...) // 拦截所有异常
    {

    }
    return 0;
}