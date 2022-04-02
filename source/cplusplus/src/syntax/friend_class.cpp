#include <iostream>

using namespace std;

/* 
    友元演示
    1、
    2、
*/


class Point
{
    friend class Other;
    friend void func(Point &point);

public:
    Point(const int &x, const int &y)
    {
        this->m_x = x;
        this->m_y = y;
    }
    int getX()
    {
        return m_x;
    }

    int getY()
    {
        return m_y;
    }

private:
    int m_x;
    int m_y;
};

class Other
{
    void add(Point &p1, Point &p2)
    {
        int x = p1.m_x + p2.m_y;
        int y = p1.m_y + p2.m_y;
        cout << (x + y) << endl;
    }
};

void func(Point &point)
{
    cout << point.getX() << ", " << point.getY() << endl;
    cout << point.m_x << ", " << point.m_y << endl;
}

int main()
{
    Point p(10, 20);
    Point p2(20, 30);
    func(p);
    func(p2);
    return 0;
}