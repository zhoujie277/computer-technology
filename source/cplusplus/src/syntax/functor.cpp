#include <iostream>
using namespace std;

class Sum
{
public:
    int operator()(int a, int b)
    {
        return a + b;
    }
};

int main()
{
    Sum sum;
    int s = sum(3, 4);
    cout << s << endl;
    return 0;
}