#include <iostream>
using namespace std;
/**
 * 多态
 * c++中多态建立在虚函数之上，需要实现多态的函数必须用关键字virtual修饰，
 * virtual修饰之后，将会在类的头8个字节存放一个虚函数表地址。
 * 虚函数表中存放真实需要调用的virtual函数的地址。(如果没重写，就是父类的函数地址，如果重写了，就是自己的函数地址)
 */
class Animal
{
public:
    // int sex;
    // int age;
    // int hello;
    virtual void speak()
    {
    }
    virtual void run()
    {
    }
    virtual void test()
    {
    }
};

class Cat : public Animal
{
    void speak()
    {
        cout << "Cat speak" << endl;
    }

    void run()
    {
        cout << "Cat run" << endl;
    }
    void test()
    {
        cout << "test" << endl;
    }
};

class Dog : public Animal
{
    void speak()
    {
        cout << "Dog speak" << endl;
    }
    void run()
    {
        cout << "Dog run" << endl;
    }
    void test()
    {
        cout << "test" << endl;
    }
};

void speak(Animal *animal)
{
    animal->speak();
    animal->run();
    animal->test();
}

int main()
{
    Dog dog;
    speak(&dog);
    Cat cat;
    speak(&cat);

    cout << "sizeof(Dog): " << sizeof(dog) << " bytes " << endl;
    cout << "sizeof(Cat):" << sizeof(cat) << " bytes " << endl;
    cout << "sizeof(Animal):" << sizeof(Animal) << " bytes " << endl;
    return 0;
}