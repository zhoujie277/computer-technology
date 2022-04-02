#include <iostream>
using namespace std;

// 局部类
void test()
{
    int age = 10;
    class Test
    {
        void run()
        {
            // cout << age << endl;
        }
    };
}

// 内部类和定义在外部类的区别仅仅是访问权限变了，其它均没有改变。
class OuterClass
{
public:
    class InnerClass
    {
        void run()
        {

        }
    };
};

int main()
{
}