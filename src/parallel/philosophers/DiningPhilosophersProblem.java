package parallel.philosophers;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Flatware {
    private boolean taken = false;

    synchronized void take() throws InterruptedException {
        while (taken) {
            wait();
        }
        taken = true;
    }

    synchronized void drop() {
        taken = false;
        notifyAll();
    }
}

class Philosopher implements Runnable {
    private Flatware left;
    private Flatware right;
    private final int id;
    private final int ponderFactor;
    private Random rand = new Random(47);

    private void pause() throws InterruptedException {
        if (ponderFactor == 0) {
            return;
        }
        TimeUnit.MILLISECONDS.sleep(rand.nextInt(ponderFactor * 250));
    }

    Philosopher(Flatware left, Flatware right, int ident, int ponder) {
        this.left = left;
        this.right = right;
        id = ident;
        ponderFactor = ponder;
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                System.out.println(this + " " + "думает");
                pause();
                //философ проголодался
                System.out.println(this + " " + "берет левую");
                left.take();
                System.out.println(this + " " + "берет правую");
                right.take();
                System.out.println(this + " " + "ест");
                pause();
                right.drop();
                left.drop();
            }
        } catch (InterruptedException e) {
            System.out.println(this + " " + "выход через прерывание");
        }
    }

    public String toString() {
        return "Философ " + id;
    }
}

public class DiningPhilosophersProblem {
    public static void main(String[] args) throws Exception {
        int ponder = 5;
        int size = 100;
        ExecutorService exec = Executors.newCachedThreadPool();
        Flatware[] flatwares = new Flatware[size];
        for (int i = 0; i < size; ++i) {
            flatwares[i] = new Flatware();
        }
        for (int i = 0; i < size; ++i) {
            if (i < (size - 1)) {
                exec.execute(new Philosopher(flatwares[i], flatwares[i+1], i, ponder));
            } else {
                exec.execute(new Philosopher(flatwares[0], flatwares[i], i, ponder));
            }
        }
        TimeUnit.SECONDS.sleep(10);
        exec.shutdownNow();
    }
}
