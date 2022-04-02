#include <iostream>
using namespace std;
/**
 * c++ std标准说明
 * 1、c++98: vs2005/
 * 2、c++11: auto/desltype
 * 3、
 */

// c++11特性
void c11_demo(int *v)
{
    int a = 10;
    auto b = 10; // c++11 特性
    int *p = nullptr;

    int array[]{11, 22, 33, 44, 55};
}

int add(int a, int b)
{
    return a + b;
}

int sub(int a, int b)
{
    return a - b;
}

int exec(int a, int b, int (*func)(int, int))
{
    return func(a, b);
}

void exec_test()
{
    int a = 3;
    int b = 4;
    cout << exec(a, b, add) << endl;
    cout << exec(a, b, sub) << endl;

    //lambda 表达式
    cout << exec(a, b, [](int x, int y)
                 { return x * y; })
         << endl;
    cout << exec(a, b, [](int x, int y)
                 { return x / y; })
         << endl;
}

// lambda 捕获
void lambda_expression_catch()
{
    cout << "-----lambda_expression_catch-----" << endl;
    int a = 10;
    // 地址捕获和值捕获, 地址捕获也可以用于值修改。
    auto func = [&a]
    {
        // a++;
        cout << a << endl;
    };
    a = 20;
    func();

    cout << "--------mutable demo---------" << endl;

    auto func1 = [a]() mutable
    {
        // 值捕获，默认不可修改。
        // 声明 mutable，可修改。但不会影响到外面的值
        a++;
        cout << "catch a'value is " << a << endl;
    };
    func1();
    cout << "a'value is " << a << endl;
}

void lambda_expression()
{
    // lambda 本质上是个函数
    ([]
     { cout << "func()....." << endl; })();

    void (*p)() = ([]
                   { cout << "p()....." << endl; });
    p();

    int (*pInt)(int, int) = ([](int a, int b) -> int
                             {
                                 cout << "pInt()....." << (a + b) << endl;
                                 return a + b;
                             });
    pInt(3, 4);
}

int main()
{
    c11_demo(nullptr);
    lambda_expression();
    exec_test();
    lambda_expression_catch();
    return 0;
}