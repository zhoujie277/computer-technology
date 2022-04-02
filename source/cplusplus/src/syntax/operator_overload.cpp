#include <iostream>

using namespace std;

class Point
{
    friend ostream &operator<<(ostream &, const Point &);
    friend istream &operator>>(istream &, Point &);

public:
    Point(const int &x, const int &y)
    {
        m_x = x;
        m_y = y;
    }

    Point(const Point &p)
    {
        cout << "......copy Constructor" << endl;
    }

    int getX()
    {
        return m_x;
    }

    int getY()
    {
        return m_y;
    }

    Point operator+(const Point &p)
    {
        return Point(m_x + p.m_x, m_y + p.m_y);
    }

    // 返回常引用，防止更改。 声明const函数，以支持链式调用。
    const Point operator-(const Point &p) const
    {
        return Point(m_x - p.m_x, m_y - p.m_y);
    }

    Point operator-()
    {
        return Point(-m_x, -m_y);
    }

    // 前置++
    void operator++()
    {
    }

    // 后置++
    void operator++(int)
    {
    }

private:
    int m_x;
    int m_y;
};

ostream &operator<<(ostream &cout, const Point &p)
{
    cout << "(" << p.m_x << ", " << p.m_y << ")" << endl;
    return cout;
}

istream &operator>>(istream &cin, Point &p)
{
    cin >> p.m_x;
    cin >> p.m_y;
    return cin;
}

int main()
{
    Point x(10, 20);
    Point y(20, 30);
    Point q(40, 50);
    Point z = x + y;
    cout << "x = " << z.getX() << ", y = " << z.getY() << endl;
    Point p = x - y - q;
    cout << p << endl;
    return 0;
}