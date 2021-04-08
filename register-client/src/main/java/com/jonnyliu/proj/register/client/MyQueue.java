package com.jonnyliu.proj.register.client;

import java.util.LinkedList;

/**
 * 阻塞队列小试牛刀
 *
 * @author liujie
 */
public class MyQueue {

    private static final int DEFAULT_CAPACITY = 10;

    private int capacity;

    private LinkedList<Object> queue;

    public MyQueue() {
        this(DEFAULT_CAPACITY);
    }

    public MyQueue(int capacity) {
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    public synchronized void add(Object element) {
        if (queue.size() == capacity) {
            try {
                System.out.println(Thread.currentThread().getName() + " 队列已满...");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(Thread.currentThread().getName() + " put...");
        queue.add(element);
        notify();
    }

    public synchronized Object take() {
        if (queue.isEmpty()) {
            System.out.println("队列为空,");
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Object poll = queue.poll();
        System.out.println(Thread.currentThread().getName() + " take...");
        notify();
        return poll;
    }

    public static void main(String[] args) {
        MyQueue queue = new MyQueue();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    queue.add(1);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "thread-put").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "thread-take").start();
    }


}
