package io.runon.stock.securities.firm.api.kor.koreainvestment;

import io.runon.commons.http.HttpApiResponse;
import io.runon.commons.utils.time.Times;
import io.runon.stock.trading.Stock;
import io.runon.stock.trading.exception.StockApiException;
import io.runon.trading.CountryCode;
import io.runon.trading.TradingTimes;
import io.runon.trading.technical.analysis.candle.TradeCandle;
import io.runon.trading.technical.analysis.candle.TradeCandleDataKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 한국 투자증권 해외주식 기간별 시세 관련 api 정리
 * @author macle
 */
public class KoreainvestmentOverseasPeriodApi {
    private final KoreainvestmentApi koreainvestmentApi;
    public KoreainvestmentOverseasPeriodApi(KoreainvestmentApi koreainvestmentApi){
        this.koreainvestmentApi = koreainvestmentApi;
    }


    public String get1mCandleJsonText(String exchange, String symbol, String ymd, String hm){
        //apiportal.koreainvestment.com/apiservice/apiservice-oversea-stock-quotations#L_852d7e45-4f34-418b-b6a1-a4552bbcdf90
        koreainvestmentApi.updateAccessToken();
        String url = "/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice";
        Map<String, String> requestHeaderMap = koreainvestmentApi.computeIfAbsenttPropertySingleMap(url,"tr_id","HHDFS76950200");

        String exchangeCode = exchange;
        if(exchangeCode.equals("NASDAQ")){
            exchangeCode = "NAS";
        }else if(exchangeCode.equals("NYSE")){
            exchangeCode = "NYS";
        }else if(exchangeCode.equals("AMEX")){
            exchangeCode = "AMS";
        }

        String query = "?AUTH=&EXCD=" + exchangeCode + "&SYMB=" + symbol + "&NMIN=1&PINC=1&NEXT=1&NREC=120&FILL=&KEYB=" +ymd+hm+"00";
        HttpApiResponse response =  koreainvestmentApi.getHttpGet().getResponse(url + query, requestHeaderMap);
        if(response.getResponseCode() != 200){
            throw new StockApiException("token make fail code:" + response.getResponseCode() +", " + response.getMessage());
        }

        return response.getMessage();
    }

    public TradeCandle [] get1mCandles(Stock stock, String ymd){
        //그날의 최대시간 호출
        String firstText = get1mCandleJsonText(stock.getExchange(), stock.getSymbol(), ymd, "2350");
        JSONObject object = new JSONObject(firstText);
        String code = object.getString("rt_cd");
        if(!code.equals("0")){
            if(!object.isNull("msg1")){
                throw new StockApiException("rt_cd: " + code + ", message: " + object.getString("msg1"));
            }else{
                throw new StockApiException("rt_cd: " + code);
            }
        }
        if(object.isNull("output2")){
            return TradeCandle.EMPTY_CANDLES;
        }

        JSONArray array = object.getJSONArray("output2");

        if(array.isEmpty()){
            return TradeCandle.EMPTY_CANDLES;
        }


        boolean isBreak = false;
        List<TradeCandle> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            TradeCandle candle = get1mCandle(array.getJSONObject(i));
            list.add(candle);


            if(!candle.getData(TradeCandleDataKey.YMDHM).startsWith(ymd)){
                isBreak =true;
                break;
            }
        }

        for(;;){
            if(isBreak){
                break;
            }
            koreainvestmentApi.minuteSleep();

            TradeCandle last = list.get(list.size()-1);
            String ymdhm = last.getData(TradeCandleDataKey.YMDHM);
            String next = Times.getYmdhm(ymdhm, -Times.MINUTE_1);
            next = Times.getHm(next);
            String jsonText = get1mCandleJsonText(stock.getExchange(), stock.getSymbol(), ymd, next);

//            System.out.println(stock.getSymbol() + " " + ymd +" " + next);

            object = new JSONObject(jsonText);
            code = object.getString("rt_cd");
            if(!code.equals("0")){
                if(!object.isNull("msg1")){
                    throw new StockApiException("rt_cd: " + code + ", message: " + object.getString("msg1"));
                }else{
                    throw new StockApiException("rt_cd: " + code);
                }
            }

            if(object.isNull("output2")){
                break;
            }

            array = object.getJSONArray("output2");
            if(array.isEmpty()){
                break;
            }

            for (int i = 0; i < array.length(); i++) {
                TradeCandle candle = get1mCandle(array.getJSONObject(i));
                list.add(candle);
                if(!candle.getData(TradeCandleDataKey.YMDHM).startsWith(ymd)){
                    isBreak =true;
                    break;
                }

            }
        }

        if(list.isEmpty()){
            return TradeCandle.EMPTY_CANDLES;
        }

        List<TradeCandle> reslut = new ArrayList<>();
        //가격 변화량 설정하기

        int first = list.size()-1;
        for (int i = first; i >-1 ; i--) {

            TradeCandle candle = list.get(i);
            if(candle.getData(TradeCandleDataKey.YMDHM).startsWith(ymd)){
                reslut.add(candle);
            }
            if(i == first){
                continue;
            }
            candle.setPrevious(list.get(i+1).getPrevious());
            candle.setChange();
            candle.setEndTrade();
        }


        return reslut.toArray(new TradeCandle[0]);
    }
    public TradeCandle get1mCandle(JSONObject row){

        String korYmd = row.getString("kymd");
        String korHms = row.getString("khms");

        String ymd = row.getString("xymd");
        String hms = row.getString("xhms");

        long openTime = Times.getTime("yyyyMMdd HHmmss", korYmd +" " + korHms, TradingTimes.KOR_ZONE_ID);
        long closeTime = openTime + Times.MINUTE_1;

        TradeCandle tradeCandle = new TradeCandle();
        tradeCandle.setOpenTime(openTime);
        tradeCandle.setCloseTime(closeTime);
        tradeCandle.setOpen(row.getBigDecimal("open"));
        tradeCandle.setHigh(row.getBigDecimal("high"));
        tradeCandle.setLow(row.getBigDecimal("low"));
        tradeCandle.setClose(row.getBigDecimal("last"));
        tradeCandle.setVolume(row.getBigDecimal("evol"));
        tradeCandle.setAmount(row.getBigDecimal("eamt"));
        tradeCandle.setEndTrade();

        tradeCandle.addData(TradeCandleDataKey.KOR_YMDHM, korYmd+" " + korHms.substring(0,4));
        tradeCandle.addData(TradeCandleDataKey.YMDHM, ymd+" " + hms.substring(0,4));
        return tradeCandle;
    }


    public String getDailyJsonText(String exchange, String symbol, String baseYmd, boolean isRevisePrice){
        //apiportal.koreainvestment.com/apiservice/apiservice-oversea-stock-quotations#L_0e9fb2ba-bbac-4735-925a-a35e08c9a790

        koreainvestmentApi.updateAccessToken();
        String url = "/uapi/overseas-price/v1/quotations/dailyprice";

        String exchangeCode = exchange;
        if(exchangeCode.equals("NASDAQ")){
            exchangeCode = "NAS";
        }else if(exchangeCode.equals("NYSE")){
            exchangeCode = "NYS";
        }else if(exchangeCode.equals("AMEX")){
            exchangeCode = "AMS";
        }

        String modp;
        if(isRevisePrice){
            modp = "1";
        }else{
            modp = "0";
        }

        Map<String, String> requestHeaderMap = koreainvestmentApi.computeIfAbsenttPropertySingleMap(url,"tr_id","HHDFS76240000");
        String query = "?AUTH=&EXCD=" + exchangeCode + "&SYMB=" + symbol + "&GUBN=0&BYMD=" + baseYmd+"&MODP="+ modp;
        HttpApiResponse response =  koreainvestmentApi.getHttpGet().getResponse(url + query, requestHeaderMap);
        if(response.getResponseCode() != 200){
            throw new StockApiException("token make fail code:" + response.getResponseCode() +", " + response.getMessage());
        }

        return response.getMessage();
    }


    public TradeCandle [] getDailyCandles(CountryCode countryCode, String jsonText){
        JSONObject object = new JSONObject(jsonText);
        String code = object.getString("rt_cd");
        if(!code.equals("0")){
            if(!object.isNull("msg1")){
                throw new StockApiException("rt_cd: " + code + ", message: " + object.getString("msg1"));
            }else{
                throw new StockApiException("rt_cd: " + code);
            }
        }


        JSONArray array = object.getJSONArray("output2");
        if(array.length() == 0){
            return TradeCandle.EMPTY_CANDLES;
        }

        int length = array.length();

        int candleIndex = 0;
        TradeCandle [] candles = new TradeCandle[length];
        for (int i = length -1; i > -1 ; i--) {
            JSONObject row = array.getJSONObject(i);

            TradeCandle tradeCandle = new TradeCandle();

            String ymd = row.getString("xymd");


            tradeCandle.setOpenTime(TradingTimes.getDailyOpenTime(countryCode, ymd));
            tradeCandle.setCloseTime(TradingTimes.getDailyCloseTime(countryCode, ymd));
            tradeCandle.setOpen(new BigDecimal(row.getString("open")));
            tradeCandle.setHigh(new BigDecimal(row.getString("high")));
            tradeCandle.setLow(new BigDecimal(row.getString("low")));
            tradeCandle.setClose(new BigDecimal(row.getString("clos")));
            tradeCandle.setVolume(new BigDecimal(row.getString("tvol")));
            tradeCandle.setAmount(new BigDecimal(row.getString("tamt")));
            tradeCandle.setChange(new BigDecimal(row.getString("diff")));

            tradeCandle.setChange();
            tradeCandle.setEndTrade();
            candles[candleIndex++] = tradeCandle;
        }

        return candles;
    }


}
