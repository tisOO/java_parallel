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

class SimpleBookStore {

    private LinkedList<Good> books = new LinkedList<Good>();

    SimpleBookStore(int count) {
        for (Good book : new GoodGenerator(count)) {
            books.add(book);
        }
    }

    /**
     * Просмотр товара по его ID
     *
     * @param index Индекс товара
     * @return Описание товара
     * @throws IndexOutOfBoundsException
     */
    public synchronized String readBookByIndex(int index) throws IndexOutOfBoundsException {
        if (index > books.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        return books.get(index).toString();
    }

    /**
     * Сжигаем книгу
     * если книги больше нет, то удаляем его из магазина
     *
     * @param index Индекс товара
     * @return Название товара
     * @throws IndexOutOfBoundsException
     */
    public synchronized String fireBookByIndex(int index) throws IndexOutOfBoundsException {
        if (index > books.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        Good book = books.get(index);
        if (book.getQuantity() > 0) {
            book.setQuantity(book.getQuantity() - 1);
        }
        if (book.getQuantity() < 1) {
            books.remove(index);
        }

        return book.toString();
    }

    /**
     * Добавляем книгу
     * @param index
     * @return
     */
    public synchronized String addBookByIndex(int index) throws IndexOutOfBoundsException {
        if (index > books.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        Good book = books.get(index);
        if (book.getQuantity() < Integer.MAX_VALUE) {
            book.setQuantity(book.getQuantity() + 1);
        } else {
            return "To much books here: " + book.toString();
        }

        return book.toString();
    }

    /**
     * Товары в продаже
     *
     * @return Возвращает количество товаров, которые еще в продаже
     */
    public synchronized int getGoodsCount() {
        return books.size();
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
class SimpleReader implements Runnable {
    private Random random = new Random();
    private volatile SimpleBookStore bookStore;
    private volatile OwnReadWriteLock readWriteLock;

    SimpleReader(SimpleBookStore bookStore, OwnReadWriteLock readWriteLock) {
        this.bookStore = bookStore;
        this.readWriteLock = readWriteLock;
    }

    public void run() {
        try {
            while (true) {
                try {
                    readWriteLock.lockRead();
                    if (bookStore.getGoodsCount() < 1) {
                        readWriteLock.unlockRead();
                        break;
                    }
                    System.out.println(bookStore.readBookByIndex(random.nextInt(bookStore.getGoodsCount())));
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
class SimpleWriter implements Runnable {
    private Random random = new Random();
    private volatile SimpleBookStore bookStore;
    private volatile OwnReadWriteLock readWriteLock;

    SimpleWriter(SimpleBookStore bookStore, OwnReadWriteLock readWriteLock) {
        this.bookStore = bookStore;
        this.readWriteLock = readWriteLock;
    }

    synchronized void action() {
        if (random.nextBoolean()) {
            System.out.println("Writer: " + bookStore.fireBookByIndex(random.nextInt(bookStore.getGoodsCount())));
        } else {
            System.out.println("Writer: " + bookStore.fireBookByIndex(random.nextInt(bookStore.getGoodsCount())));
        }

    }

    public void run() {
        try {
            while (true) {
                try {
                    readWriteLock.lockWrite();
                    if (bookStore.getGoodsCount() < 1) {
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

        SimpleBookStore market = new SimpleBookStore(productsCount);
        OwnReadWriteLock readWriteLock = new OwnReadWriteLock();

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new SimpleReader(market, readWriteLock)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new SimpleWriter(market, readWriteLock)).start();
        }
    }
}
