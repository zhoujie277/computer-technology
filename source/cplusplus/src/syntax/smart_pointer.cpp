#include <iostream>
using namespace std;

/* 
    智能指针：
    1、auto_ptr：缺点，不能用数组，不建议使用。c++11已废弃
    2、shared_ptr：允许多个引用同时指向，本质上会产生强引用计数，多指针变量超出作用域，强引用计数就会减一。当饮用计数为0时，则会释放对象。
                使用时可能会产生诸多问题，如内存泄漏、计数次数与使用次数不符，导致提前释放报错。或者多次释放。
    3、weak_ptr:弱引用，不会产生强引用计数，一般搭配shared_ptr使用。
    4、unique_ptr:同一时间只允许一个指针引用指向，如果需要转移引用，可用unique_ptr::move()方法转移引用。
    5、当业务上需要有循环引用的时候，不管主从对象内部都采用弱引用的方式去使用。这样创建对象均由外部去管理。
 */

void normal()
{
    int *p = new int();
    // 需要手动释放
    delete p;
    p = nullptr;
}

// 传统指针需要注意不同条件分支的释放
void try_catch()
{
    int *p;
    try
    {
        p = new int();
        delete p;
    }
    catch (...)
    {
        delete p;
    }
}

class Person;

class Car
{
public:
    Car();
    ~Car();

public:
    weak_ptr<Person> m_person;
};

class Person
{
public:
    Person();
    Person(int age);
    ~Person();
    void run();

public:
    weak_ptr<Car> m_car;

private:
    int m_age;
};

Person::Person()
{
    m_age = 0;
    cout << "Person" << endl;
}
Person::Person(int age)
{
    m_age = age;
    cout << "Person" << endl;
}
Person::~Person()
{
    cout << "~Person" << endl;
}
void Person::run()
{
    cout << "run..." << endl;
}

Car::Car()
{
    cout << "Car constructor" << endl;
}

Car::~Car()
{
    cout << "~Car destructor" << endl;
}

void smart_one()
{
    // 智能指针p指向了堆空间的Person对象。c++11已被废弃
    // auto_ptr<Person> p(new Person(20));
    // p->run();

    // auto_ptr<Person> p(new Person[10]);// error
}

template <typename T>
class SmartPointer
{
public:
    SmartPointer(T *obj)
    {
        this->m_obj = obj;
    }
    ~SmartPointer()
    {
        if (this->m_obj == nullptr)
            return;
        delete this->m_obj;
        this->m_obj = nullptr;
    }

    T *operator->()
    {
        return m_obj;
    }

private:
    T *m_obj;
};

void test()
{
    SmartPointer<Person> p(new Person(30));
    p->run();
}

/* 
    shared_ptr的设计理念
        多个shared_ptr可以指向同一个对象，当最后一个shared_ptr在作用域范围内结束时，对象才会释放。
        可以通过一个已存在的智能指针初始化一个新的智能指针。

    shared_ptr的原理
    1、一个shared_ptr会对一个对象产生强引用(strong reference)
    2、每个对象都有个与之对应的强引用计数，记录着当前对象被多少个shared_ptr强引用着。
    3、可以通过shared_ptr的use_count函数获得强引用计数。
    4、当有一个新的shared_ptr指向对象时，对象的强引用计数就会+1;
    5、当有一个shared_ptr销毁时，比如作用域结束，对象的强引用就会-1.
*/
void shared_ptr_demo()
{
    cout << "----shared_ptr_demo----" << endl;

    // shared_ptr<Person> p(new Person(40));
    // p->run();

    // 第二个参数告诉编译器未来会如何析构
    // shared_ptr<Person> p1(new Person[5], std::default_delete<Person[]>());

    // lambda 表示式也可以
    // shared_ptr<Person> p1(new Person[5], [] (Person* p) { delete[] p});

    // used count
    {
        cout << endl
             << "enter p4 scope..." << endl;
        shared_ptr<Person> p4;
        {
            shared_ptr<Person> p1(new Person(10));

            shared_ptr<Person> p2 = p1;

            shared_ptr<Person> p3 = p2;

            p4 = p3;
            cout << " p4.use_count()=" << p4.use_count() << endl;
        }
        cout << "out p4 scope... p4.use_count() = " << p4.use_count() << endl;
    }
    cout << "shared_ptr_demo function end" << endl;
}

void multiDestructor()
{
    cout << "..multiDestructor.." << endl;
    //一下代码会执行两次析构函数，然后报错,要注意智能指针的使用
    Person *p = new Person();
    {
        shared_ptr<Person> sp(p);
    } // ~Person();
    {
        shared_ptr<Person> sp(p);
    } // ~Person(); error
}

void cyclicReference()
{
    cout << endl
         << "---- cyclicReference ----" << endl;

    {

        shared_ptr<Person> per(new Person);
        shared_ptr<Car> car(new Car);

        // 产生了内存泄漏，没有调用彼此的析构函数
        per->m_car = car;
        car->m_person = per;

        // Person *p = new Person;
        // Car *car = new Car();
        // delete p;
        // delete car;
    }
}

void unique_ptr_demo()
{
    cout << endl
         << "---- unique_ptr_demo" << endl;
    {
        unique_ptr<Person> p(new Person(10));
        unique_ptr<Person> p1 = std::move(p);
    }
    cout << "unique_ptr_demo func end" << endl;
}

int main()
{
    smart_one();
    test();
    shared_ptr_demo();
    cyclicReference();
    unique_ptr_demo();
    // multiDestructor(); // error
    return 0;
}