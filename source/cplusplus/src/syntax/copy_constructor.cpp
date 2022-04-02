#include <iostream>
using namespace std;
/* 
    拷贝构造函数：
    1、编译器默认会为类生成一个拷贝构造函数，并完成成员变量赋值。
    2、当开发者给类显示提供一个拷贝构造函数之后，会调用拷贝构造函数，但是初始化动作由开发者定义。
    3、编译器默认提供的拷贝是浅拷贝，如果有指针，最好是手动深拷贝。如果不是深拷贝，很有可能造成两个问题：
        1、一个对象free了，另外一个对象的成员也被释放了。
        2、可能被多次free。
    4、Car car = car1; Car car(car1) 都会调用拷贝构造函数
    5、用对象类型作为参数和返回值的时候会发生拷贝构造函数操作，浪费了内存。故：通常用引用或指针来作为函数参数和返回值。
    6、关于构造函数补充知识点：构造函数如果没有声明，编译器只有在初始化的时候会做一些额外的事情，才会为我们生成构造函数。
        比如：需要生成虚表地址（虚函数、虚继承、
 */

class Car
{
public:
    char *m_name;
    int m_price;
    Car()
    {
        m_name = NULL;
        m_price = 0;
    }

    // 该声明要求不允许隐式调用构造函数
    /* explicit */ Car(int price)
    {
        this->m_price = price;
        cout << "Car single constructor was called" << endl;
    }

    Car(const Car &car)
    {
        if (car.m_name)
        {
            int len = strlen(car.m_name) + 1;
            m_name = new char[len];
            strcpy(m_name, car.m_name);
        }
        m_price = car.m_price;
    }
    ~Car()
    {
        if (!m_name)
            return;
        delete[] m_name;
        m_name = NULL;
    }

    void display()
    {
        cout << m_name << endl;
    }
};

int main()
{
    char name[] = "custom name";
    Car car;
    car.m_name = new char[strlen(name) + 1];
    strcpy(car.m_name, name);
    car.m_price = 20;
    // 拷贝构造函数
    Car secondCar(car);
    secondCar.display();

    // 匿名对象
    Car();

    //隐式调用构造函数。
    Car car3 = 20;
    return 0;
}