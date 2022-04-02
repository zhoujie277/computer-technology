#include <iostream>
using namespace std;
/* 
    static关键字
    1、静态成员变量必须在类外初始化。
    2、如果声明和实现分离，则静态成员必须在cpp文件内初始化。
    3、static成员变量存储在数据区，所有对象共享。
    4、静态成员函数和成员变量均可通过对象名、对象指针、类名作用域访问。
    5、静态成员函数中没有this。(this指针只能用于非静态成员函数内)
 */
class Car
{
public:
    static int factor;
    int m_price;

    void run()
    {
        cout << "Car run..." << endl;
    }
};

/* 单例实现 */
class SingleInstance
{
public:
    static SingleInstance *instance()
    {
        static SingleInstance instance;
        return &instance;
    }

private:
    SingleInstance(const SingleInstance& instance){}
    void operator=(const SingleInstance& instance);
    SingleInstance() {}
    ~SingleInstance() {}
};

// 必须在类外初始化
int Car::factor = 0;

int main()
{
    Car car;
    Car::factor = 1;
    cout << "Car:: access static variable factor = " << Car::factor << endl;
    SingleInstance *p1 = SingleInstance::instance();
    SingleInstance *p2 = SingleInstance::instance();
    cout << "SingleInstance'address is " << p1 << ", the instance isSingle? " << (p1 == p2) << endl;
    return 0;
}
