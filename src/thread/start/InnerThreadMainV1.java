package thread.start;

import static util.MyLogger.log;

public class InnerThreadMainV1 {

    public static void main(String[] args) {
        log("main() start");

        MyRunnable runnable = new MyRunnable();

        Thread thread = new Thread(runnable);
        thread.start();

        log("main() end");
    }

    static class MyRunnable implements Runnable {

        @Override
        public void run() {
            log(Thread.currentThread().getName() + ": run()");
        }
    }

}
