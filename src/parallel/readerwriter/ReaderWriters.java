package parallel.readerwriter;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.*;

class BookStore {

    private LinkedList<Good> books = new LinkedList<Good>();

    BookStore(int count) {
        for (Good good : new GoodGenerator(count)) {
            books.add(good);
        }
    }

    /**
     * Просмотр товара по его ID
     *
     * @param index Индекс товара
     * @return Описание товара
     * @throws IndexOutOfBoundsException
     */
    String readBookByIndex(int index) {

        if (index > books.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        return books.get(index).toString();
    }

    int getBooksCount() {

        return books.size();

    }

    /**
     * Сжигаем книгу
     * если книги больше нет, то удаляем его из магазина
     *
     * @param index Индекс товара
     * @return Название товара
     * @throws IndexOutOfBoundsException
     */
    String fireBookByIndex(int index) throws IndexOutOfBoundsException {
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
    String addBookByIndex(int index) throws IndexOutOfBoundsException {
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
}

class Writer implements Runnable {
    volatile private BookStore bookStore;
    private Random random = new Random();

    private final Lock write;

    Writer(BookStore bookStore, Lock write) {
        this.bookStore = bookStore;
        this.write = write;
    }

    public void run() {
        try {
            synchronized (bookStore) {
                while (true) {
                    try {
                        write.lock();
                        if (bookStore.getBooksCount() < 1) {
                            break;
                        }
                        if (random.nextBoolean()) {
                            System.out.println("Add book: " + bookStore.addBookByIndex(random.nextInt(bookStore.getBooksCount())));
                        } else {
                            System.out.println("Fire book: " + bookStore.fireBookByIndex(random.nextInt(bookStore.getBooksCount())));
                        }
                    } finally {
                        write.unlock();
                    }

                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Something went wrong: " + e);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
}

class Reader implements Runnable {
    private Random random = new Random();
    volatile private BookStore bookStore;

    private final Lock read;


    Reader(BookStore bookStore, Lock read) {
        this.bookStore = bookStore;
        this.read = read;
    }

    public void run() {
        try {
            while (true) {
                try {
                    read.lock();
                    if (bookStore.getBooksCount() < 1) {
                        break;
                    }
                    System.out.println(bookStore.readBookByIndex(random.nextInt(bookStore.getBooksCount())));
                } finally {
                    read.unlock();
                }

            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Something went wrong: " + e);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}

public class ReaderWriters {

    public static void main(String[] args) {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Lock write = readWriteLock.writeLock();
        Lock read = readWriteLock.readLock();
        int productsCount = 100;
        int visitorCount = 400;
        int buyerCount = 1;
        if (args.length > 0) {
            productsCount = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            visitorCount = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            buyerCount = Integer.parseInt(args[2]);
        }

        BookStore bookStore = new BookStore(productsCount);

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new Reader(bookStore, read)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new Writer(bookStore, write)).start();

        }
    }
}
