#include <iostream>
using namespace std;
/* 
    菱形继承：
    1、菱形继承中，存在一个问题，顶级父类存在一个成员变量。会分别被Father、Mother继承，最后，会被Son继承。
        所以，son的size会是自己的 m_pen\m_money\m_cookbook\m_age\m_age。5个int类型，共20个字节。
        并且，当son使用m_age时，产生了歧义。
    2、解决该问题可采用虚继承法。虚继承会使的Father对象和Mother对象都会生成一个虚表指针，Sun会生成两个虚表指针，并且虚表当中会存放两个偏移，
        一个是虚表指针与本类起始地址的偏移量。一个是虚基类第一个成员变量与起始地址的偏移量。可使Sun无论从哪个虚表地址都能准确的找到Person的age。
        
 */
class Person
{
public:
    int m_age;
    Person() : m_age(1)
    {
    }
};

class Father : virtual public Person
{
public:
    int m_money;
    Father() : m_money(2)
    {
    }
};

class Mother : virtual public Person
{
public:
    int m_cookbook;
    Mother() : m_cookbook(3)
    {
    }
};

class Son : public Father, public Mother
{
public:
    int m_pen;
    Son() : m_pen(4)
    {
    }
};

int main()
{
    Son son;
    // son.m_age = 10; // 产生二义性，故报错。
    cout << "sizeOf(son) is " << sizeof(Son) << endl; //没有采用虚继承时，输出20，因为age被继承了两次。

    return 0;
}