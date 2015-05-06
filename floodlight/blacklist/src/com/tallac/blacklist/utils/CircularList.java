/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */
package com.tallac.blacklist.utils;

import java.util.ArrayList;
import java.util.List;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class CircularList<Element>
{
    //private static final Logger LOG = LoggerFactory.getLogger(CircularList.class);

    private final int mCapacity;
    private final ArrayList<Element> mList;
    private int mHead;
    private int mTail;

    public CircularList(int capacity)
    {
        mCapacity = capacity;
        mList = new ArrayList<Element>();
        mHead = 0;
        mTail = 0;
    }

    /**
     * Add element to the circular list
     * @param el - new element
     */
    public void add(Element el)
    {
        if (size() == mCapacity)
        {
            // Recalculate head and tail if size is equal capacity
            mTail = mHead;
            mHead = getListIndex(mHead + 1);

            //Replace tail element in the list
            mList.set(mTail, el);
        }
        else
        {
            // Move tail if size is still below capacity
            mTail +=1;
            // Add element to the array
            mList.add(el);
        }
    }

    /**
     * Get list
     * @return ordered list
     */
    public List<Element> get()
    {
        List<Element> retList = new ArrayList<Element>(size());

        for (int i = 0; i < size(); i++)
        {
            retList.add(i, getElement(i));
        }

        return retList;
    }

    /**
     * Get list size
     * @return list size
     */
    public int size()
    {
        return mList.size();
    }

    /**
     * Calculate index of element in the list
     * @param n - calculate index in the list
     * @return index in the list
     */
    private int getListIndex(int n)
    {
        int retVal = n % mCapacity;

        // java modulus may be negative
        if (retVal < 0)
        {
            retVal += mCapacity;
        }

        return retVal;
    }

    /**
     * Get target element from the list
     * @param n - element number
     * @return - Target element
     */
    private Element getElement(int n)
    {
        if (n < 0 || n > mCapacity - 1)
        {
            throw new IndexOutOfBoundsException();
        }
        return mList.get(getListIndex(mHead + n));
    }
}
