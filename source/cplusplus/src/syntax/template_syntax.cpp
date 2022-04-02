#include <iostream>
using namespace std;

/**
 * c++模版原理
 * 编译器根据你调用模版函数的参数类型，生成不同类型的函数。
 */
class Point
{
public:
    Point()
    {
    }

    Point(const int &x, const int &y)
    {
        m_x = x;
        m_y = y;
    }

    const Point operator+(const Point &p) const
    {
        return Point(m_x + p.m_x, m_y + p.m_y);
    }

    int getX()
    {
        return m_x;
    }
    int getY() { return m_y; }

private:
    int m_x;
    int m_y;
};

// int add(int a, int b)
// {
//     return a + b;
// }

// double add(double a, double b)
// {
//     return a + b;
// }

// Point add(Point& a, Point& b) {
//     return a + b;
// }

template <typename T>
T add(const T &a, const T &b)
{
    return a + b;
}

int main()
{
    int ia = add<int>(1, 3);
    Point p1(1, 3);
    Point p2(4, 5);
    Point ip = add<Point>(p1, p2);
    cout << "x = " << ip.getX() << ", y = " << ip.getY() << endl;
    return 0;
}