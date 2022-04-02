#include <iostream>
using namespace std;

/**
 * 多继承演示
 * 1、如果有多个父类，并且父类有虚函数，则在子类对象头部会有多个地址，每个地址表示一个虚表地址。多个虚表地址相互独立。
 * 2、如果有命名冲突：用作用域符号表示区分，如：子类对象名.父类名称::方法
 */
class Father
{
public:
    int m_age;

    virtual void run()
    {
        cout << "Father run...sizeof(Father)=" << sizeof(Father) << endl;
    }
};

class Mother
{
public:
    int m_age;

    virtual void run()
    {
        cout << "Mother run... sizeof(Mother)=" << sizeof(Mother) << endl;
    }
};

class Son : public Father, public Mother
{
public:
    int m_age;

    void run()
    {
        cout << "Son run... sizeof(Son)=" << sizeof(Son) << endl;
    }
};

int main()
{
    Son son;
    son.run();
    son.Father::run();
    son.Mother::run();
    return 0;
}