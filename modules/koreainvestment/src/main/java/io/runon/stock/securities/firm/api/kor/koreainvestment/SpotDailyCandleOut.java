package io.runon.stock.securities.firm.api.kor.koreainvestment;

import com.seomse.commons.config.JsonFileProperties;
import com.seomse.commons.utils.ExceptionUtil;
import com.seomse.commons.utils.time.Times;
import com.seomse.commons.utils.time.YmdUtil;
import io.runon.stock.trading.Stock;
import io.runon.stock.trading.Stocks;
import io.runon.stock.trading.data.management.StockDailyOutParam;
import io.runon.stock.trading.path.StockPathLastTime;
import io.runon.stock.trading.path.StockPathLastTimeCandle;
import io.runon.stock.trading.path.StockPaths;
import io.runon.trading.TradingTimes;
import io.runon.trading.data.csv.CsvCandle;
import io.runon.trading.data.csv.CsvTimeFile;
import io.runon.trading.data.file.FileLineOut;
import io.runon.trading.data.file.PathTimeLine;
import io.runon.trading.data.file.TimeName;
import io.runon.trading.technical.analysis.candle.Candles;
import io.runon.trading.technical.analysis.candle.TradeCandle;
import lombok.extern.slf4j.Slf4j;


/**
 * 현물 일봉 캔들 내리기
 * 한국 투자증권은 아직 분봉과거치를 지원하지 않음
 * 올해 지원예정 중이라고 하였음
 * @author macle
 */
@Slf4j
public class SpotDailyCandleOut {

    protected final KoreainvestmentApi koreainvestmentApi;
    protected final KoreainvestmentPeriodDataApi periodDataApi;

    private  String [] exchanges = {
            "KOSPI"
            , "KOSDAQ"
    };

    private final StockDailyOutParam stockDailyOutParam = new StockDailyOutParam() {
        @Override
        public String[] getLines(Stock stock, String beginYmd, String endYmd) {
            String text = periodDataApi.getPeriodDataJsonText(stock.getSymbol(),"D", beginYmd, endYmd, true);
            TradeCandle [] candles = KoreainvestmentPeriodDataApi.getCandles(text);
            return CsvCandle.lines(candles);
        }

        @Override
        public void sleep() {
            koreainvestmentApi.candleOutSleep();
        }

        @Override
        public PathTimeLine getPathTimeLine() {
            return PathTimeLine.CSV;
        }

        @Override
        public StockPathLastTime getStockPathLastTime() {
            return StockPathLastTime.CANDLE;
        }

        @Override
        public String[] getExchanges() {
            return exchanges;
        }

        @Override
        public JsonFileProperties getJsonFileProperties() {
            return koreainvestmentApi.getJsonFileProperties();
        }

        @Override
        public String getDeletedPropertiesKey() {
            return "delisted_stocks_candle_1d";
        }
    };


    public SpotDailyCandleOut(KoreainvestmentApi koreainvestmentApi){
        this.koreainvestmentApi = koreainvestmentApi;
        this.periodDataApi = koreainvestmentApi.getPeriodDataApi();

    }

    public SpotDailyCandleOut(){
        this.koreainvestmentApi = KoreainvestmentApi.getInstance();
        this.periodDataApi = koreainvestmentApi.getPeriodDataApi();
    }



    private final StockPathLastTime stockPathLastTime = new StockPathLastTimeCandle();

    public void setExchanges(String[] exchanges) {
        this.exchanges = exchanges;
    }

    public void outKor(){
        //전체 종목 일봉 내리기
        //KONEX 는 제외
        String [] exchanges = {
                "KOSPI"
                , "KOSDAQ"
        };

        Stock [] stocks = Stocks.getStocks(exchanges);

        Stocks.sortUseLastTimeParallel(stocks,"1d", stockPathLastTime);

        for(Stock stock : stocks){
            try {
                //같은 데이터를 호출하면 호출 제한이 걸리는 경우가 있다 전체 캔들을 내릴때는 예외처리를 강제해서 멈추지 않는 로직을 추가
                out(stock);
            }catch (Exception e){
                try{
                    Thread.sleep(5000L);
                }catch (Exception ignore){}
                log.error(ExceptionUtil.getStackTrace(e) +"\n" + stock);
            }
        }
    }
    
    //상폐된 주식 캔들 내리기
    public void outKorDelisted(){
        String [] exchanges = {
                "KOSPI"
                , "KOSDAQ"
        };


        JsonFileProperties jsonFileProperties = koreainvestmentApi.getJsonFileProperties();

        String delistedYmd = jsonFileProperties.getString("delisted_stocks_candle_1d","19900101");

        String nowYmd = YmdUtil.now(TradingTimes.KOR_ZONE_ID);

        Stock [] stocks = Stocks.getDelistedStocks(exchanges, delistedYmd, nowYmd);
        for(Stock stock : stocks){
            try {
                //같은 데이터를 호출하면 호출 제한이 걸리는 경우가 있다 전체 캔들을 내릴때는 예외처리를 강제해서 멈추지 않는 로직을 추가
                out(stock);
            }catch (Exception e){
                try{
                    Thread.sleep(5000L);
                }catch (Exception ignore){}
                log.error(ExceptionUtil.getStackTrace(e) +"\n" + stock);
            }
        }

        jsonFileProperties.set("delisted_stocks_candle_1d", nowYmd);

    }

    /**
     * 상장 시점부터 내릴 수 있는 전체 정보를 내린다.
     * @param stock 종목정보
     */
    public void out(Stock stock){


        String nowYmd = YmdUtil.now(TradingTimes.KOR_ZONE_ID);
        int nowYmdNum = Integer.parseInt(nowYmd);
        KoreainvestmentPeriodDataApi periodDataApi = koreainvestmentApi.getPeriodDataApi();

        //초기 데이터는 상장 년원일
        String nextYmd ;

        String filesDirPath = StockPaths.getSpotCandleFilesPath(stock.getStockId(),"1d");

        long lastTime = CsvTimeFile.getLastTime(filesDirPath);

        if(lastTime > -1){
            nextYmd = YmdUtil.getYmd(lastTime, TradingTimes.KOR_ZONE_ID);

            if(stock.getDelistedYmd() != null){
                int lastYmdInt = Integer.parseInt(nextYmd);
                if(lastYmdInt >= stock.getDelistedYmd()){
                    //상폐종목인경우 이미 캔들이 다 저장되어 있을때
                    return ;
                }
            }

        }else{
            if(stock.getListedYmd() == null){
                log.error("listed ymd null: " + stock);
                return ;
            }
            nextYmd = Integer.toString(stock.getListedYmd());

        }

        TimeName.Type timeNameType = TimeName.getCandleType(Times.DAY_1);

        boolean isFirst = true;


        log.debug("start stock: " + stock);

        int maxYmd = nowYmdNum;

        if(stock.getDelistedYmd() != null){
            maxYmd = stock.getDelistedYmd();
        }
        //최대100건
        for(;;){

            if(YmdUtil.compare(nextYmd, nowYmd) > 0){
                break;
            }

            String endYmd = YmdUtil.getYmd(nextYmd, 100);

            int endYmdNum =  Integer.parseInt(endYmd);
            if(endYmdNum > maxYmd){
                endYmd = Integer.toString(maxYmd);
            }

            String text = periodDataApi.getPeriodDataJsonText(stock.getSymbol(),"D", nextYmd, endYmd, true);
            TradeCandle [] candles = KoreainvestmentPeriodDataApi.getCandles(text);

            String [] lines = CsvCandle.lines(candles);

            if(isFirst) {

                FileLineOut.outBackPartChange(PathTimeLine.CSV, lines, filesDirPath, timeNameType, TradingTimes.KOR_ZONE_ID);
                isFirst = false;
            }else{
                FileLineOut.outNewLines(PathTimeLine.CSV, lines, filesDirPath, timeNameType, TradingTimes.KOR_ZONE_ID);
            }

            if(endYmdNum >= maxYmd){
                break;
            }

            if(candles.length == 0){
                nextYmd = YmdUtil.getYmd(endYmd, 1);
            }else{
                nextYmd = YmdUtil.getYmd(Candles.getMaxYmd(candles, TradingTimes.KOR_ZONE_ID),1);
            }

            koreainvestmentApi.candleOutSleep();
        }
    }

    public static void main(String[] args) {

//        jsonFileProperties.set("delisted_stocks_ymd","20240501");
        String [] exchanges = {
                "KOSPI"
                , "KOSDAQ"
        };

        Stock [] stocks = Stocks.getDelistedStocks(exchanges, "20240503", "20240503");

        for(Stock stock : stocks){
            System.out.println(stock);
        }
        System.out.println(stocks.length);

//        SpotDailyCandleOut spotDailyCandleOut = new SpotDailyCandleOut();
//
//        KoreainvestmentApi.getInstance();
    }

}
