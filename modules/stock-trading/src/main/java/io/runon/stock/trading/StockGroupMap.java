package io.runon.stock.trading;

import com.seomse.jdbc.annotation.Column;
import com.seomse.jdbc.annotation.Table;
import com.seomse.jdbc.annotation.PrimaryKey;
import com.seomse.jdbc.annotation.DateTime;
import io.runon.trading.TradingGson;
import lombok.Data;

/**
 * @author macle
 */
@Data
@Table(name="stock_group_map")
public class StockGroupMap {

    @PrimaryKey(seq = 1)
    @Column(name = "stock_group_id")
    String stockGroupId;

    @PrimaryKey(seq = 2)
    @Column(name = "stock_id")
    String stockId;

    @DateTime
    @Column(name = "updated_at")
    long updatedAt = System.currentTimeMillis();

    @Override
    public String toString(){
        return  TradingGson.LOWER_CASE_WITH_UNDERSCORES_PRETTY.toJson(this);
    }
}