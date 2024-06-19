package io.runon.stock.securities.firm.api.kor.koreainvestment;

import com.seomse.commons.http.HttpApiResponse;
import com.seomse.commons.utils.time.Times;
import com.seomse.commons.utils.time.YmdUtil;
import io.runon.stock.trading.exception.StockApiException;
import io.runon.trading.*;
import io.runon.trading.technical.analysis.candle.TradeCandle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 한국투자증권 기간별 데이터 관련 API 정의
 * API가 많아서 정리한 클래스를 나눈다.
 * @author macle
 */
public class KoreainvestmentPeriodDataApi {

    private final KoreainvestmentApi koreainvestmentApi;
    public KoreainvestmentPeriodDataApi(KoreainvestmentApi koreainvestmentApi){
        this.koreainvestmentApi = koreainvestmentApi;
    }

    /**
     *
     *  fid_cond_mrkt_div_code J : 주식, ETF, ETN
     *
     * @param symbol 종목코드
     *
     * @param period 기간유형 	D:일봉, W:주봉, M:월봉, Y:년봉
     * @param beginYmd 시작년월일
     * @param endYmd 끝 년월일
     * @param isRevisePrice 수정주가 여뷰
     * @return 결과값 jsontext
     */
    public String getPeriodDataJsonText(String symbol, String period, String beginYmd, String endYmd, boolean isRevisePrice){
        //https://apiportal.koreainvestment.com/apiservice/apiservice-domestic-stock-quotations#L_a08c3421-e50f-4f24-b1fe-64c12f723c77

        koreainvestmentApi.updateAccessToken();
        String url = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        Map<String, String> requestHeaderMap = koreainvestmentApi.computeIfAbsenttPropertySingleMap(url,"tr_id","FHKST03010100");

        //수정주가여부
        String sendRevisePrice;
        if(isRevisePrice){
            sendRevisePrice = "0";
        }else{
            sendRevisePrice = "1";
        }

        String query = "?fid_cond_mrkt_div_code=J&fid_input_iscd=" + symbol +"&fid_input_date_1=" + beginYmd +"&fid_input_date_2=" +endYmd +"&fid_period_div_code=" + period + "&fid_org_adj_prc=" + sendRevisePrice;

        HttpApiResponse response =  koreainvestmentApi.getHttpGet().getResponse(url + query, requestHeaderMap);
        if(response.getResponseCode() != 200){
            throw new StockApiException("code:" + response.getResponseCode() +", " + response.getMessage() +", symbol: " + symbol +", beginYmd: " + beginYmd);
        }

        return response.getMessage();
    }

    /**
     * 일별 신용 정보
     * @param symbol 종복코드
     * @param ymd 결제일자
     * @return 결과값 json
     */
    public String getDailyCreditLoanJson(String symbol, String ymd){

        koreainvestmentApi.updateAccessToken();
        String url = "/uapi/domestic-stock/v1/quotations/daily-credit-balance";
        Map<String, String> requestHeaderMap = koreainvestmentApi.computeIfAbsenttPropertySingleMap(url,"tr_id","FHPST04760000");

        String query = "?fid_cond_mrkt_div_code=J&fid_cond_scr_div_code=20476&fid_input_iscd=" + symbol +"&fid_input_date_1=" +ymd ;
        HttpApiResponse response =  koreainvestmentApi.getHttpGet().getResponse(url + query, requestHeaderMap);
        if(response.getResponseCode() != 200){
            throw new StockApiException("token make fail code:" + response.getResponseCode() +", " + response.getMessage());
        }
        return response.getMessage();
    }


    public CreditLoanDaily[] getCreditLoanDailies(String symbol, String beginYmd, String endYmd){


        List<CreditLoanDaily> list = new ArrayList<>();


        String nextBeginYmd = beginYmd;

        int endYmdNum = Integer.parseInt(endYmd);
        String dateFormat = "yyyyMMdd hh:mm";

        for(;;){

            int beginYmdNum = Integer.parseInt(nextBeginYmd);

            String callYmd = YmdUtil.getYmd(nextBeginYmd, 30);
            if(YmdUtil.compare(callYmd, endYmd) > 0){
                callYmd = endYmd;
            }

            String jsonText = getDailyCreditLoanJson(symbol, callYmd);

            JSONObject object = new JSONObject(jsonText);
            String code = object.getString("rt_cd");
            if(!code.equals("0")){
                if(!object.isNull("msg1")){
                    throw new StockApiException("rt_cd: " + code + ", message: " + object.getString("msg1"));
                }else{
                    throw new StockApiException("rt_cd: " + code);
                }
            }

            JSONArray array = object.getJSONArray("output");
            int length = array.length();
            for (int i = length -1; i > -1 ; i--) {

                CreditLoanDaily creditLoanDaily = new CreditLoanDaily();

                JSONObject row = array.getJSONObject(i);

                String tradeYmd = row.getString("deal_date");

                int tradeYmdInt = Integer.parseInt(tradeYmd);

                if(tradeYmdInt < beginYmdNum){
                    continue;
                }

                if(tradeYmdInt > endYmdNum){
                    break;
                }

                creditLoanDaily.setTime(Times.getTime(dateFormat, tradeYmd +" 09:00", TradingTimes.KOR_ZONE_ID));

                creditLoanDaily.setTradeYmd(tradeYmdInt);
                creditLoanDaily.setPaymentYmd(Integer.parseInt(row.getString("stlm_date")));

                creditLoanDaily.setLoanNewQuantity(new BigDecimal(row.getString("whol_loan_new_stcn")));
                creditLoanDaily.setLoanRepaymentQuantity(new BigDecimal(row.getString("whol_loan_rdmp_stcn")));
                creditLoanDaily.setLoanBalanceQuantity(new BigDecimal(row.getString("whol_loan_rmnd_stcn")));

                creditLoanDaily.setLoanNewAmount(new BigDecimal(row.getString("whol_loan_new_amt")));
                creditLoanDaily.setLoanRepaymentAmount(new BigDecimal(row.getString("whol_loan_rdmp_amt")));
                creditLoanDaily.setLoanBalanceAmount(new BigDecimal(row.getString("whol_loan_rmnd_amt")));

                creditLoanDaily.setLoanBalanceRate(new BigDecimal(row.getString("whol_loan_rmnd_rate")));
                creditLoanDaily.setLoanTradeRate(new BigDecimal(row.getString("whol_loan_gvrt")));

                creditLoanDaily.setClose(new BigDecimal(row.getString("stck_prpr")));
                creditLoanDaily.setOpen(new BigDecimal(row.getString("stck_oprc")));
                creditLoanDaily.setHigh(new BigDecimal(row.getString("stck_hgpr")));
                creditLoanDaily.setLow(new BigDecimal(row.getString("stck_lwpr")));
                creditLoanDaily.setVolume(new BigDecimal(row.getString("acml_vol")));

                list.add(creditLoanDaily);

            }

            if(YmdUtil.compare(callYmd, endYmd) >= 0){
                break;
            }

            nextBeginYmd = YmdUtil.getYmd(callYmd,1);
        }
        if(list.size() == 0){
            return CreditLoans.EMPTY_DAILY_ARRAY;
        }



        return list.toArray(new CreditLoanDaily[0]);
    }


    public TradeCandle [] getCandles(String symbol, String period, String beginYmd, String endYmd, boolean isRevisePrice){
        String jsonText = getPeriodDataJsonText(symbol, period, beginYmd, endYmd, isRevisePrice);
        return getCandles(jsonText);
    }

    public static TradeCandle [] getCandles(String jsonText){

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

        String dateFormat = "yyyyMMdd hh:mm";

        int length = array.length();

        int candleIndex = 0;
        TradeCandle [] candles = new TradeCandle[length];

        for (int i = length -1; i > -1 ; i--) {

            JSONObject row = array.getJSONObject(i);

            if(row.isNull("stck_bsop_date")){
                //상장 이전데이터를 조회할경우
                return TradeCandle.EMPTY_CANDLES;
            }

            String ymd = row.getString("stck_bsop_date");

            TradeCandle tradeCandle = new TradeCandle();
            tradeCandle.setOpenTime(Times.getTime(dateFormat, ymd +" 09:00", TradingTimes.KOR_ZONE_ID));
            tradeCandle.setCloseTime(Times.getTime(dateFormat, ymd +" 15:30", TradingTimes.KOR_ZONE_ID));
            tradeCandle.setOpen(new BigDecimal(row.getString("stck_oprc")));
            tradeCandle.setHigh(new BigDecimal(row.getString("stck_hgpr")));
            tradeCandle.setLow(new BigDecimal(row.getString("stck_lwpr")));
            tradeCandle.setClose(new BigDecimal(row.getString("stck_clpr")));
            tradeCandle.setVolume(new BigDecimal(row.getString("acml_vol")));
            tradeCandle.setTradingPrice(new BigDecimal(row.getString("acml_tr_pbmn")));
            tradeCandle.setChange(new BigDecimal(row.getString("prdy_vrss")));

            //락 유형
            if(!row.isNull("flng_cls_code")) {
                String clsCode = row.getString("flng_cls_code");
                if(!clsCode.equals("00")){
                    tradeCandle.addData("lock_code", clsCode);

//                     * 01 : 권리락
//                     * 02 : 배당락
//                     * 03 : 분배락
//                     * 04 : 권배락
//                     * 05 : 중간(분기)배당락
//                     * 06 : 권리중간배당락
//                     * 07 : 권리분기배당락

                    if(clsCode.equals("01")){
                        tradeCandle.addData("lock_type", LockType.RIGHTS_LOCK.toString());
                    }else if(clsCode.equals("02")){
                        tradeCandle.addData("lock_type", LockType.DIVIDEND_LOCK.toString());
                    }else if(clsCode.equals("03")){
                        tradeCandle.addData("lock_type", LockType.DISTRIBUTION_LOCK.toString());
                    }else if(clsCode.equals("04")){
                        tradeCandle.addData("lock_type", LockType.RIGHTS_DIVIDEND_LOCK.toString());
                    }else if(clsCode.equals("05")){
                        tradeCandle.addData("lock_type", LockType.DIVIDEND_LOCK.toString());
                    }else if(clsCode.equals("06")){
                        tradeCandle.addData("lock_type", LockType.RIGHTS_DIVIDEND_LOCK.toString());
                    }else if(clsCode.equals("07")){
                        tradeCandle.addData("lock_type", LockType.RIGHTS_DIVIDEND_LOCK.toString());
                    }
                }
            }
            if(!row.isNull("prdy_vrss_sign")) {
                String signValue = row.getString("prdy_vrss_sign");
                if(signValue.equals("1")){
                    tradeCandle.setPriceChangeType(PriceChangeType.RISE);
                    tradeCandle.setPriceLimit(true);
                }else if(signValue.equals("2")){
                    tradeCandle.setPriceChangeType(PriceChangeType.RISE);
                }else if(signValue.equals("3")){
                    tradeCandle.setPriceChangeType(PriceChangeType.HOLD);
                }else if(signValue.equals("4")){
                    tradeCandle.setPriceChangeType(PriceChangeType.FALL);
                    tradeCandle.setPriceLimit(true);
                }else if(signValue.equals("5")){
                    tradeCandle.setPriceChangeType(PriceChangeType.FALL);
                }
            }

            tradeCandle.setChange();
            tradeCandle.setEndTrade();
            candles[candleIndex++] = tradeCandle;

        }

        return candles;
    }

    //매매동향 (기관 외국인, ) 이베스트는 투신 사모펀드 등 다양하게 제공하지만 한국 투자증권은 기관계 정도만 제공함 우선 한투를 이용하고 관련 데이터의 상세한 분석이 필요할때 활용
    //    apiportal.koreainvestment.com/apiservice/apiservice-domestic-stock-quotations2#L_e27baf2f-6ec0-4029-b4fd-4c873f340478
    // 프로그램 매매
    //종목별 매수합, 매도합 ( 체결강도 계산), 기존 켄들 데이터에 업데이트 로직필요함


//
//    //apiportal.koreainvestment.com/apiservice/apiservice-domestic-stock-Manalysis#L_0cc848c0-4928-4b89-bca4-62df430e4a45
//    public String getDailyMarketInvestorJson(String market, String symbol, String ymd){
/////uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market
////        Format
//
//        market = market.toUpperCase();
//        if(market.equals("KOSPI")){
//            market = "KSP";
//        }else if(market.equals("KOSDAQ")){
//            market ="KSQ";
//        }
//
//        koreainvestmentApi.updateAccessToken();
//        String url = "uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market";
//        Map<String, String> requestHeaderMap = koreainvestmentApi.computeIfAbsenttPropertySingleMap(url,"tr_id","FHPTJ04040000");
//
//
//        String query = "?FID_COND_MRKT_DIV_CODE=U&FID_INPUT_ISCD=" + symbol +"&FID_INPUT_DATE_1=" + ymd +"&FID_INPUT_ISCD_1=" +market;
//
//        HttpApiResponse response =  koreainvestmentApi.getHttpGet().getResponse(url + query, requestHeaderMap);
//        if(response.getResponseCode() != 200){
//            throw new KoreainvestmentApiException("code:" + response.getResponseCode() +", " + response.getMessage() +", symbol: " + symbol +", market: " + beginYmd);
//        }
//
//        return response.getMessage();
//
//    }


}