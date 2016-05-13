package parallel.readerwriter;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.*;

class SmartMarket {

    private LinkedList<Good> goods = new LinkedList<Good>();

    SmartMarket(int count) {
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
    String showGoodByIndex(int index) {

        if (index > goods.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        return goods.get(index).toString();
    }

    int getGoodsCount() {

        return goods.size();

    }

    /**
     * Продаем по одному товару в руки,
     * если товар закончился, то удаляем его из магазина
     *
     * @param index Индекс товара
     * @return Название товара
     * @throws IndexOutOfBoundsException
     */
    String buyGoodByIndex(int index) {
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
}

class SmartBuyer implements Runnable {
    volatile private SmartMarket market;
    private Random random = new Random();

    private final Lock write;

    SmartBuyer(SmartMarket market, Lock write) {
        this.market = market;
        this.write = write;
    }

    public void run() {
        try {
            synchronized (market) {
                while (market.getGoodsCount() > 0) {
                    try {
                        write.lock();
                        if (market.getGoodsCount() < 1) {
                            break;
                        }
                        System.out.println("Order: " + market.buyGoodByIndex(random.nextInt(market.getGoodsCount())));
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

class SmartVisitor implements Runnable {
    private Random random = new Random();
    volatile private SmartMarket market;

    private final Lock read;


    SmartVisitor(SmartMarket market, Lock read) {
        this.market = market;
        this.read = read;
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                try {
                    read.lock();
                    if (market.getGoodsCount() < 1) {
                        break;
                    }
                    System.out.println(market.showGoodByIndex(random.nextInt(market.getGoodsCount())));
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
        int visitorCount = 40;
        int buyerCount = 800;
        if (args.length > 0) {
            productsCount = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            visitorCount = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            buyerCount = Integer.parseInt(args[2]);
        }

        SmartMarket market = new SmartMarket(productsCount);

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new SmartVisitor(market, read)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new SmartBuyer(market, write)).start();

        }
    }
}
