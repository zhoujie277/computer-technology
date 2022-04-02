#include <iostream>
using namespace std;
/* 
    const关键字的使用
    1、const作为类的成员变量使用：必须在声明的时候进行初始化。
    2、声明常量：const int a = 4;
    3、声明常指针：const int *p = &a;
    4、声明指针常量：int * const p = &a;
    5、const成员函数。限制函数内部不能修改非静态函数变量。也不可调用非const成员函数。可调用static成员函数。
    6、const对象变量，只能调用const成员函数，或者静态成员函数。详见Person
 */
class Person
{
public:
    //const 成员变量必须在声明的时候进行初始化。
    const int mc_age;

    Person(int age) : mc_age(age)
    {
    }
    // const 成员函数
    void run() const
    {
        // age = 5; // error
        // eat();// error
        cout << "run..." << endl;
    }

    void run()
    {
        cout << "non const run..." << endl;
    }

    void eat()
    {
    }
};

int main()
{
    Person p(10);
    p.run();

    const Person person(20);
    person.run(); // 只能调用const成员函数，如果不是const成员函数，则会编译报错。

    int b = 5;
    const int a = 4;
    const int *p1 = &a;
    int *const p2 = &b;
}