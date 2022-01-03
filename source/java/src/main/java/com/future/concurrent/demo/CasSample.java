package com.future.concurrent.demo;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class CasSample {

    interface Account {
        int getBalance();

        void withdraw(int amount);
    }

    static void demo(Account account) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new Thread(() -> account.withdraw(10)));
        }

        long start = System.nanoTime();
        list.forEach(Thread::start);
        list.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        long end = System.nanoTime();
        log.debug("account.balance: {}, cost time {}",  account.getBalance(), end - start);
    }

    static class CasAccount implements Account {
        AtomicInteger balance;
        public CasAccount(int balance) {
            this.balance = new AtomicInteger(balance);
        }

        @Override
        public int getBalance() {
            return balance.get();
        }

        @Override
        public void withdraw(int amount) {
//            balance.getAndAdd(-amount);
            int expect, newValue;
            do {
                expect = balance.get();
                newValue = expect - amount;
            } while (!balance.compareAndSet(expect, newValue));
        }
    }

    public static void main(String[] args) {
        Account account = new CasAccount(10000);
        demo(account);
    }

}
