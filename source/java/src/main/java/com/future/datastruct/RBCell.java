package com.future.datastruct;

import java.util.Comparator;

/**
 * 该代码源自
 * http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java
 * ConcurrentHashMap 的参考代码
 *
 * @author future
 */
@SuppressWarnings("unused")
public class RBCell implements Cloneable {

    static final boolean RED = false;
    static final boolean BLACK = true;

    /**
     * The element held in the node
     **/
    private Object element_;

    /**
     * The node color (RED, BLACK)
     **/
    private boolean color_ = BLACK;

    /**
     * Pointer to left child
     **/
    private RBCell left_ = null;

    /**
     * Pointer to right child
     **/
    private RBCell right_ = null;

    /**
     * Pointer to parent (null if root)
     **/
    private RBCell parent_ = null;

    /**
     * Make a new cell with given element, null links, and BLACK color.
     * Normally only called to establish a new root.
     **/
    public RBCell(Object element) {
        element_ = element;
    }

    /**
     * Return a new RBCell with same element and color as self,
     * but with null links. (Since it is never OK to have
     * multiple identical links in a RB tree.)
     **/
    protected Object clone() throws CloneNotSupportedException {
        RBCell t = (RBCell) (super.clone());
        t.left_ = null;
        t.right_ = null;
        t.parent_ = null;
        t.element_ = element_;
        t.color_ = color_;
        return t;
    }

    /**
     * return the element value
     **/
    public final Object element() {
        return element_;
    }

    /**
     * set the element value
     **/
    public final void element(Object v) {
        element_ = v;
    }

    /**
     * Return left child (or null)
     **/
    public final RBCell left() {
        return left_;
    }

    /**
     * Return right child (or null)
     **/
    public final RBCell right() {
        return right_;
    }

    /**
     * Return parent (or null)
     **/
    public final RBCell parent() {
        return parent_;
    }

    /**
     * Return color of node p, or BLACK if p is null
     **/
    static boolean colorOf(RBCell p) {
        return (p == null) ? BLACK : p.color_;
    }

    /**
     * return parent of node p, or null if p is null
     **/
    static RBCell parentOf(RBCell p) {
        return (p == null) ? null : p.parent_;
    }

    /**
     * Set the color of node p, or do nothing if p is null
     **/
    static void setColor(RBCell p, boolean c) {
        if (p != null) p.color_ = c;
    }

    /**
     * return left child of node p, or null if p is null
     */
    static RBCell leftOf(RBCell p) {
        return (p == null) ? null : p.left_;
    }

    /**
     * return right child of node p, or null if p is null
     */
    static RBCell rightOf(RBCell p) {
        return (p == null) ? null : p.right_;
    }

    /**
     * Copy all content fields from another node
     * Override this if you add any other fields in subclasses.
     */
    protected void copyContents(RBCell t) {
        element_ = t.element_;
    }

    /**
     * Return the minimum element of the current (sub)tree
     **/
    public final RBCell leftmost() {
        RBCell p = this;
        while (p.left_ != null) {
            p = p.left_;
        }
        return p;
    }

    /**
     * Return the maximum element of the current (sub)tree
     **/
    public final RBCell rightmost() {
        RBCell p = this;
        while (p.right_ != null) {
            p = p.right_;
        }
        return p;
    }

    /**
     * Return the root (parentless node) of the tree
     **/
    public final RBCell root() {
        RBCell p = this;
        while (p.parent_ != null) {
            p = p.parent_;
        }
        return p;
    }

    /**
     * Return true if node is a root (i.e., has a null parent)
     **/
    public final boolean isRoot() {
        return parent_ == null;
    }

    /**
     * Return the inorder successor, or null if no such
     **/
    public final RBCell successor() {
        if (right_ != null)
            return right_.leftmost();
        else {
            RBCell p = parent_;
            RBCell ch = this;
            while (p != null && ch == p.right_) {
                ch = p;
                p = p.parent_;
            }
            return p;
        }
    }

    /**
     * Return the inorder predecessor, or null if no such
     **/
    public final RBCell predecessor() {
        if (left_ != null)
            return left_.rightmost();
        else {
            RBCell p = parent_;
            RBCell ch = this;
            while (p != null && ch == p.left_) {
                ch = p;
                p = p.parent_;
            }
            return p;
        }
    }

    /**
     * Return the number of nodes in the subtree
     **/
    public final int size() {
        int c = 1;
        if (left_ != null) c += left_.size();
        if (right_ != null) c += right_.size();
        return c;
    }

    /**
     * Return node of current subtree containing element as element(),
     * if it exists, else null.
     * Uses Comparator cmp to find and to check equality.
     **/
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RBCell find(Object element, Comparator cmp) {
        RBCell t = this;
        for (; ; ) {
            int diff = cmp.compare(element, t.element());
            if (diff == 0) return t;
            else if (diff < 0) t = t.left_;
            else t = t.right_;
            if (t == null) return null;
        }
    }

    /**
     * Return number of nodes of current subtree containing element.
     * Uses Comparator cmp to find and to check equality.
     **/
    @SuppressWarnings({"rawtypes", "unchecked"})
    public int count(Object element, Comparator cmp) {
        int c = 0;
        RBCell t = this;
        while (t != null) {
            int diff = cmp.compare(element, t.element());
            if (diff == 0) {
                ++c;
                if (t.left_ == null)
                    t = t.right_;
                else if (t.right_ == null)
                    t = t.left_;
                else {
                    c += t.right_.count(element, cmp);
                    t = t.left_;
                }
            } else if (diff < 0) t = t.left_;
            else t = t.right_;
        }
        return c;
    }

    /**
     * an undocumented feature for performance testing
     **/
    public int maxDepth() {
        int ld = 0;
        int rd = 0;
        if (left_ != null) ld = left_.maxDepth();
        if (right_ != null) rd = right_.maxDepth();
        int md = Math.max(ld, rd);
        return 1 + md;
    }

    /**
     * Return a new subtree containing each element of current subtree
     **/
    public RBCell copyTree() {
        RBCell t = null;
        try {
            t = (RBCell) (clone());
            if (left_ != null) {
                t.left_ = left_.copyTree();
                t.left_.parent_ = t;
            }
            if (right_ != null) {
                t.right_ = right_.copyTree();
                t.right_.parent_ = t;
            }
        } catch (CloneNotSupportedException ignored) {
        }
        return t;
    }

    /**
     * Insert cell as the left child of current node, and then
     * rebalance the tree it is in.
     *
     * @param cell  the cell to add
     * @param root, the root of the current tree
     * @return the new root of the current tree. (Rebalancing
     * can change the root!)
     **/
    public RBCell insertLeft(RBCell cell, RBCell root) {
        left_ = cell;
        cell.parent_ = this;
        return cell.fixAfterInsertion(root);
    }

    /**
     * Insert cell as the right child of current node, and then
     * rebalance the tree it is in.
     *
     * @param cell  the cell to add
     * @param root, the root of the current tree
     * @return the new root of the current tree. (Rebalancing
     * can change the root!)
     **/
    public RBCell insertRight(RBCell cell, RBCell root) {
        right_ = cell;
        cell.parent_ = this;
        return cell.fixAfterInsertion(root);
    }

    /**
     * Delete the current node, and then rebalance the tree it is in
     *
     * @param root the root of the current tree
     * @return the new root of the current tree. (Rebalancing
     * can change the root!)
     **/
    public RBCell delete(RBCell root) {
        // if strictly internal, swap contents with successor and then delete it
        if (left_ != null && right_ != null) {
            RBCell s = successor();
            copyContents(s);
            return s.delete(root);
        }

        // Start fixup at replacement node, if it exists
        RBCell replacement = (left_ != null) ? left_ : right_;
        if (replacement != null) {
            // link replacement to parent
            replacement.parent_ = parent_;
            if (parent_ == null) root = replacement;
            else if (this == parent_.left_) parent_.left_ = replacement;
            else parent_.right_ = replacement;

            // null out links so they are OK to use by fixAfterDeletion
            left_ = null;
            right_ = null;
            parent_ = null;
            // fix replacement
            if (color_ == BLACK)
                root = replacement.fixAfterDeletion(root);
            return root;
        } else if (parent_ == null) // exit if we are the only node
            return null;
        else { //  if no children, use self as phantom replacement and then unlink
            if (color_ == BLACK)
                root = this.fixAfterDeletion(root);
            // Unlink  (Couldn't before since fixAfterDeletion needs parent ptr)
            if (parent_ != null) {
                if (this == parent_.left_)
                    parent_.left_ = null;
                else if (this == parent_.right_)
                    parent_.right_ = null;
                parent_ = null;
            }
            return root;
        }
    }

    /**
     * From CLR
     **/
    protected final RBCell rotateLeft(RBCell root) {
        RBCell r = right_;
        right_ = r.left_;
        if (r.left_ != null) r.left_.parent_ = this;
        r.parent_ = parent_;
        if (parent_ == null) root = r;
        else if (parent_.left_ == this) parent_.left_ = r;
        else parent_.right_ = r;
        r.left_ = this;
        parent_ = r;
        return root;
    }

    /**
     * From CLR
     **/
    protected final RBCell rotateRight(RBCell root) {
        RBCell l = left_;
        left_ = l.right_;
        if (l.right_ != null) l.right_.parent_ = this;
        l.parent_ = parent_;
        if (parent_ == null) root = l;
        else if (parent_.right_ == this) parent_.right_ = l;
        else parent_.left_ = l;
        l.right_ = this;
        parent_ = l;
        return root;
    }

    /**
     * From CLR
     **/
    protected final RBCell fixAfterInsertion(RBCell root) {
        color_ = RED;
        RBCell x = this;

        while (x != null && x != root && x.parent_.color_ == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                RBCell y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        root = x.rotateLeft(root);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    if (parentOf(parentOf(x)) != null)
                        root = parentOf(parentOf(x)).rotateRight(root);
                }
            } else {
                RBCell y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        root = x.rotateRight(root);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    if (parentOf(parentOf(x)) != null)
                        root = parentOf(parentOf(x)).rotateLeft(root);
                }
            }
        }
        root.color_ = BLACK;
        return root;
    }


    /**
     * From CLR
     **/
    protected final RBCell fixAfterDeletion(RBCell root) {
        RBCell x = this;
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                RBCell sib = rightOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    root = parentOf(x).rotateLeft(root);
                    sib = rightOf(parentOf(x));
                }
                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        root = sib.rotateRight(root);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    root = parentOf(x).rotateLeft(root);
                    x = root;
                }
            } else { // symmetric
                RBCell sib = leftOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    root = parentOf(x).rotateRight(root);
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        root = sib.rotateLeft(root);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    root = parentOf(x).rotateRight(root);
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
        return root;
    }
}


















