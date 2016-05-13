package parallel.readerwriter;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.*;

class SmartMarket {
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private LinkedList<Good> goods = new LinkedList<Good>();

    private final Lock read = readWriteLock.readLock();
    private final Lock write = readWriteLock.writeLock();

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
    public String showGoodByIndex(int index) {
        read.lock();
        try {
            if (index > goods.size() - 1) {
                throw new IndexOutOfBoundsException();
            }
            return goods.get(index).toString();
        } finally {
            read.unlock();
        }
    }

    public int getGoodsCount() {
        read.lock();
        try {
            return goods.size();
        } finally {
            read.unlock();
        }

    }

    /**
     * Продаем по одному товару в руки,
     * если товар закончился, то удаляем его из магазина
     *
     * @param index Индекс товара
     * @return Название товара
     * @throws IndexOutOfBoundsException
     */
    public String buyGoodByIndex(int index) {
        write.lock();
        try {
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
        } finally {
            write.unlock();
        }

    }
}

class SmartBuyer implements Runnable {
    private SmartMarket market;
    private Random random = new Random();

    SmartBuyer(SmartMarket market) {
        this.market = market;
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                System.out.println("Order: " + market.buyGoodByIndex(random.nextInt(market.getGoodsCount())));
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
    private SmartMarket market;

    SmartVisitor(SmartMarket market) {
        this.market = market;
    }

    public void run() {
        try {
            while (market.getGoodsCount() > 0) {
                System.out.println(market.showGoodByIndex(random.nextInt(market.getGoodsCount())));
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
        int productsCount = 1000;
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

        SmartMarket market = new SmartMarket(productsCount);

        for (int i = 0; i < visitorCount; ++i) {
            new Thread(new SmartVisitor(market)).start();
        }
        for (int i = 0; i < buyerCount; ++i) {
            new Thread(new SmartBuyer(market)).start();

        }
    }
}
