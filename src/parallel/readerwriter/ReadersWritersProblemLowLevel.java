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
class OwnReadWriteLock {
    private volatile int readers = 0;
    private volatile int writers = 0;
    private volatile int writeRequests = 0;

    public synchronized void incReader() {
        readers++;
    }

    synchronized void lockRead() throws InterruptedException {
        while (writers > 0 || writeRequests > 0 || readers > 0) {
            wait();
        }
        readers++;
    }

    synchronized void unlockRead() throws InterruptedException {
        readers--;
        notifyAll();
    }

    synchronized void lockWrite() throws InterruptedException {
        writeRequests++;

        while (readers > 0 || writers > 0) {
            wait();
        }
        writeRequests--;
        writers++;
    }

    synchronized void unlockWrite() throws InterruptedException {
        writers--;
        notifyAll();
    }

    synchronized void decReaders() {
        readers--;
        notifyAll();
    }

    synchronized void decWriters() {
        writers--;
        notifyAll();
    }
}


/**
 * Занимается только просмотром товаров (Читатель)
 */
class Visitor implements Runnable {
    private Random random = new Random();
    private volatile Market market;
    private volatile OwnReadWriteLock readWriteLock;

    Visitor(Market market, OwnReadWriteLock readWriteLock) {
        this.market = market;
        this.readWriteLock = readWriteLock;
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                try {
                    readWriteLock.lockRead();
                    if (market.getGoodsCount() < 1) {
                        readWriteLock.unlockRead();
                        break;
                    }
                    System.out.println(market.showGoodByIndex(random.nextInt(market.getGoodsCount())));
                    readWriteLock.unlockRead();
                } catch (InterruptedException e) {
                    System.out.println(e.toString());
                    readWriteLock.decReaders();
                }
            }
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
    private volatile OwnReadWriteLock readWriteLock;

    Buyer(Market market, OwnReadWriteLock readWriteLock) {
        this.market = market;
        this.readWriteLock = readWriteLock;
    }

    synchronized void action() {
        System.out.println("Order: " + market.buyGoodByIndex(random.nextInt(market.getGoodsCount())));
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                try {
                    readWriteLock.lockWrite();
                    if (market.getGoodsCount() < 1) {
                        readWriteLock.unlockWrite();
                        break;
                    }
                    action();
                    readWriteLock.unlockWrite();
                } catch (InterruptedException e) {
                    System.out.println(e.toString());
                    readWriteLock.decWriters();
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Something went wrong: " + e);
        }
    }
}


public class ReadersWritersProblemLowLevel {

    public static void main(String[] args) {

        int productsCount = 100;
        int visitorCount = 2;
        int buyerCount = 32;
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
        OwnReadWriteLock readWriteLock = new OwnReadWriteLock();

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new Visitor(market, readWriteLock)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new Buyer(market, readWriteLock)).start();
        }
    }
}
