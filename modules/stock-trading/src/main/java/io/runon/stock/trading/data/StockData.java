package io.runon.stock.trading.data;

import io.runon.stock.trading.Stock;
/**
 * @author macle
 */
public interface StockData {

    Stock getStock(String id);

    Stock[] getStocks(String [] exchanges, String [] types);

}
