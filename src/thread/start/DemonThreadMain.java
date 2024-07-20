package thread.start;

/**
 * 백 그라운드 작업
 */
public class DemonThreadMain {

    public static void main(String[] args) {

        System.out.println(Thread.currentThread().getName() + ": main() start");
        DemonThread demonThread = new DemonThread();
        demonThread.setDaemon(true); // 데몬 스레드 여부
        demonThread.start();

        System.out.println(Thread.currentThread().getName() + ": main() end");
    }

    static class DemonThread extends Thread {
        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + ": run()");

            try {
                Thread.sleep(10000); // 10 초간 실행
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println(Thread.currentThread().getName() + ": run() end");
        }
    }
}
