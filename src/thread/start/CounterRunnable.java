package thread.start;

import static util.MyLogger.log;

public class CounterRunnable implements Runnable {

    @Override
    public void run() {
        for (int i = 1; i < 6; i++) {

            log("value : " + i);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }
    }
}
