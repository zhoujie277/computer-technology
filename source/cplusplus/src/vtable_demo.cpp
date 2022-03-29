#include <iostream>
using namespace std;

class N
{
public:
    int m_Age;
};

class Node
{
public:
    int m_Age;

public:
    void run()
    {
        cout << "run..." << endl;
    }

public:
    virtual void print()
    {
        cout << "node print.." << endl;
    }
};

class BNode : public Node
{
public:
    void print()
    {
        cout << "BNode print" << endl;
    }
};

int main()
{
    uint nsize = sizeof(N);
    uint size = sizeof(Node);
    uint bsize = sizeof(BNode);
    cout << "sizeof N (without vtable): " << nsize << endl;
    cout << "sizeof Node (has vtable): " << size << endl;
    cout << "sizeof BNode: " << bsize << endl;

    BNode node;
    Node *b = new BNode;
    node.print();
    return 0;
}