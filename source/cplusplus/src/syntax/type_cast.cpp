#include <iostream>
using namespace std;
/* 
    类型转换
    1、static_cast: 
    2、dynamic_cast: 
    3、reinterpret_cast: 
    4、const_cast: 
*/

class Person
{
public:
    int m_age;
    virtual void run() {}
};

class Man : public Person
{
public:
    void run()
    {
        cout << "man run ...." << endl;
    }
};

void static_cast_func()
{
    int a = 10;
    // 和普通强制转换没有区别。
    double b = static_cast<double>(a);
}

void const_cast_func()
{
    // 将const转成非const
    const Person *p1 = new Person();
    // *p1->m_age = 10; // error
    // Person *p2 = p1; // 默认情况const转非const error
    Person *p2 = const_cast<Person *>(p1); // const_cast 仅仅欺骗编译器，本质上p2指的还是原来p1所指向的地址。
    Person *p3 = (Person *)p1;             // 和上面效果一样，生成的汇编代码也一样。
    p2->m_age = 20;
}

void reinterpret_cast_func()
{
    int a = 10;
    double b = a;                             // b=10, 但不是简单的二进制拷贝。a的二进制0A000000, b是浮点数，特殊处理xmm0，浮点数寄存器
    double d = reinterpret_cast<double &>(a); // 直接二进制拷贝，最后 d != 10;
    cout << "double b is " << b << endl;
    cout << "double d is " << d << endl;
    int *p1 = (int *)0x10;
    // int *p2 = reinterpret_cast<int *>(0x10);
    // cout << "p2 value is " << *p2 << endl;
}

void dynamic_cast_func()
{
    Person *p1 = new Person();
    Person *p2 = new Man();
    cout << "p1 address is " << p1 << endl;
    cout << "p2 address is " << p2 << endl;
    // 如果person和man不构成多态关系，则报错，至少有虚函数，才构成多态。
    // dynamic_cast 提供运行时检测
    Man *m1 = dynamic_cast<Man *>(p1); // 不安全，将m1赋值为空指针。
    Man *m2 = dynamic_cast<Man *>(p2); // 安全
    cout << "m1 address is " << m1 << endl;
    cout << "m2 address is " << m2 << endl;
    // c语言强转，骗编译器，直接将p1的地址值赋给m3；
    Man *m3 = (Man *)p1;
}

int main()
{
    cout << "dynamic_cast_func" << endl;
    dynamic_cast_func();
    cout << "reinterpret_cast_func" << endl;
    reinterpret_cast_func();
    return 0;
}
