#include <iostream>
using namespace std;

/**
 * 析构函数演示：
 * 1、如果是子类指针指向子类对象，在delete 指针时，会先调用子类析构函数，再调用父类析构函数
 * 2、如果是父类指针指向子类对象，在delete 指针时，默认不会调用子类析构函数，需将父类析构函数声明为virtual。
 *    才会在delete指针时，先调用子类析构函数，再调用父类析构函数。
 * 3、只要顶级父类声明了virtual，默认子孙类都是virtual，无需再次声明。
 */ 
class Animal
{
public:
    virtual void run() = 0;
    virtual ~Animal()
    {
        cout << "~Animal Destrutor... " << endl;
    }
};

class Dog : public Animal
{
public:
    void run()
    {
        cout << "Dog run" << endl;
    }
    ~Dog()
    {
        cout << "~Dog Destructor" << endl;
    }
};

class Hashiqi : public Dog {
public:
    void run()
    {
        cout << "Hashiqi run" << endl;
    }
    ~Hashiqi()
    {
        cout << "~Hashiqi Destructor" << endl;
    }
};

int main()
{
    // Dog *animal = new Dog(); // 默认会先调用子类析构函数，再调用父类析构函数
    Animal *animal = new Dog();
    animal->run();
    delete animal;
    Dog* dog = new Hashiqi;
    dog->run();
    delete dog;
    return 0;
}