package parallel.readerwriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Класс товаров
 */
class Good {
    private volatile int id;
    private volatile int quantity;
    private volatile String properties;

    /**
     * Создание товара
     *
     * @param id         Идентификатор
     * @param quantity   Количество
     * @param properties Свойства товара
     */
    Good(int id, int quantity, String properties) {
        this.id = id;
        this.quantity = quantity;
        this.properties = properties;
    }

    public int getId() {
        return id;
    }

    public synchronized int getQuantity() {
        return quantity;
    }

    public synchronized void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getProperties() {
        return this.properties;
    }

    public synchronized String toString() {
        return "ID: " + id + ", Quantity: " + quantity + ", properties: '" + properties + "'";
    }

}

/**
 * Генератор товара
 */
class GoodGenerator implements Iterable<Good> {
    private static Random random = new Random(47); // for testing

    public GoodGenerator() {
    }

    private int size = 5;


    public GoodGenerator(int size) {
        this.size = size;
    }

    private final int MAX_GOOD_QUANTITY = 10;

    private static int goodsCount = 0;

    public Good next() {
        try {
            return new Good(++goodsCount, MAX_GOOD_QUANTITY, "testValue");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class GoodIterator implements Iterator<Good> {
        int count = size;

        public boolean hasNext() {
            return count > 0;
        }

        public Good next() {
            count--;
            return GoodGenerator.this.next();
        }

    }

    public Iterator<Good> iterator() {
        return new GoodIterator();
    }
}

class Market {

    private volatile LinkedList<Good> goods = new LinkedList<Good>();

    Market(int count) {
        for (Good good : new GoodGenerator(count)) {
            goods.add(good);
        }
    }

    /**
     * Просмотр товара по его ID
     *
     * @param index Индекс товара
     * @return Описание товара
     * @throws IndexOutOfBoundsException
     */
    public synchronized String showGoodByIndex(int index) throws IndexOutOfBoundsException {
        if (index > goods.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        return goods.get(index).toString();
    }

    /**
     * Продаем по одному товару в руки,
     * если товар закончился, то удаляем его из магазина
     *
     * @param index Индекс товара
     * @return Название товара
     * @throws IndexOutOfBoundsException
     */
    public synchronized String buyGoodByIndex(int index) throws IndexOutOfBoundsException {
        if (index > goods.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        Good good = goods.get(index);
        if (good.getQuantity() > 0) {
            good.setQuantity(good.getQuantity() - 1);
        }
        if (good.getQuantity() < 1) {
            goods.remove(index);
        }

        return good.toString();
    }

    /**
     * Товары в продаже
     *
     * @return Возвращает количество товаров, которые еще в продаже
     */
    public synchronized int getGoodsCount() {
        return goods.size();
    }
}

/**
 * Что-то вроде семафора
 */
class Semaphore {
    volatile int readers = 0;
    volatile int writers = 0;

    public synchronized void incReader() {
        readers++;
    }

    public synchronized void decReaders() {
        readers--;
        notifyAll();
    }

    public synchronized int getReaders() {
        return readers;
    }

    /**
     * Добавляем писателя, только в том случае, если в этот момент нет читателей
     */
    public synchronized void incWriters() {
        if (readers == 0) {
            writers++;
        }
    }

    public synchronized void decWriters() {
        writers--;
        notifyAll();
    }

    public synchronized int getWriters() {
        return writers;
    }

    public synchronized boolean isReadOnly() {
        return readers > 0 && writers < 1;
    }

    public synchronized void loaf() throws InterruptedException {
        wait();
    }
}


/**
 * Занимается только просмотром товаров (Читатель)
 */
class Visitor implements Runnable {
    private Random random = new Random();
    private volatile Market market;
    private Semaphore semaphore;

    Visitor(Market market, Semaphore semaphore) {
        this.market = market;
        this.semaphore = semaphore;
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                semaphore.incReader();
                if (semaphore.isReadOnly()) {
                    System.out.println(market.showGoodByIndex(random.nextInt(market.getGoodsCount())));
                    semaphore.decReaders();
                } else {
                    semaphore.loaf();
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Something went wrong: " + e);
        }
    }
}

/**
 * Занимается покупкой товаров (Писатель)
 */
class Buyer implements Runnable {
    private Random random = new Random();
    private volatile Market market;
    private Semaphore semaphore;

    Buyer(Market market, Semaphore semaphore) {
        this.market = market;
        this.semaphore = semaphore;
    }

    public synchronized void action() throws InterruptedException {
        while (market.getGoodsCount() > 0) {
            if (!semaphore.isReadOnly()) {
                semaphore.incWriters();
                System.out.println("Order: " + market.buyGoodByIndex(random.nextInt(market.getGoodsCount())));
                semaphore.decWriters();
            } else {
                semaphore.loaf();
            }
        }
    }

    public void run() {
        try {
            action();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Something went wrong: " + e);
        }
    }
}


public class ReadersWritersProblemLowLevel {

    public static void main(String[] args) {

        int productsCount = 10;
        int visitorCount = 4;
        int buyerCount = 8;
        if (args.length > 0) {
            productsCount = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            visitorCount = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            buyerCount = Integer.parseInt(args[2]);
        }

        Market market = new Market(productsCount);
        Semaphore semaphore = new Semaphore();

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new Visitor(market, semaphore)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new Buyer(market, semaphore)).start();
        }
    }
}
