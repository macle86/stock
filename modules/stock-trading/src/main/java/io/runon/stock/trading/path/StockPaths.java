package io.runon.stock.trading.path;

import com.seomse.commons.config.Config;
import io.runon.stock.trading.Stocks;
import io.runon.trading.CountryCode;
import io.runon.trading.TradingConfig;

import java.nio.file.FileSystems;

/**
 * 주식 분석에 사용하는 경료확인용 유틸성 클래스
 * @author macle
 */
public class StockPaths {



    @SuppressWarnings("ConstantValue")
    public static String getSpotCandlePath(){
        String nullCode = null;
        return getSpotCandlePath(nullCode);
    }

    public static String getSpotCandlePath(CountryCode countryCode){
        return getSpotCandlePath(countryCode.toString());
    }

    /**
     * 주식 캔들은 종목수가 많아서 국가별로 따로 관리할 수 있는 기능을 추가 한다. (용량분산)
     * 한국 종목만 3천개가 넘고, 미국까지 은 2만개가량의 종목 정보가 저장될 수 있다.
     * @param countryCode 국가코드
     * @return 캔들 폴더 경로
     */
    public static String getSpotCandlePath(String countryCode){
        return getSpotDirPath(countryCode, "stock.spot.candle.dir.path", "candle");
    }

    public static String getSpotCandleFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        return  getSpotCandlePath(Stocks.getCountryCode(stockId))+fileSeparator+stockId+fileSeparator+interval;
    }

    public static String getSpotCreditLoanPath(CountryCode countryCode){
        return getSpotCreditLoanPath(countryCode.toString());
    }

    public static String getSpotCreditLoanPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.spot.credit.loan.dir.path", "credit_loan");
    }
    //신용
    public static String getSpotCreditLoanFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        return getSpotCreditLoanPath(Stocks.getCountryCode(stockId))+fileSeparator+stockId+fileSeparator+interval;
    }

    //대주
    public static String getStockLoanFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String countryCode = Stocks.getCountryCode(stockId);
        return getStockLoanPath((countryCode)) + fileSeparator + stockId + fileSeparator + interval;
    }

    //대주
    public static String getStockLoanPath(CountryCode countryCode){
        return getStockLoanPath(countryCode.toString());
    }

    //대주
    public static String getStockLoanPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.loan.dir.path", "stock_loan");
    }


    //공매도
    public static String getShortSellingFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String countryCode = Stocks.getCountryCode(stockId);
        return getShortSellingPath((countryCode)) + fileSeparator + stockId + fileSeparator + interval;
    }

    //공매도
    public static String getShortSellingPath(CountryCode countryCode){
        return getShortSellingPath(countryCode.toString());
    }

    //공매도
    public static String getShortSellingPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.short.selling.dir.path", "short_selling");
    }

    //매매동향 외국인기관
    public static String getInvestorFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String countryCode = Stocks.getCountryCode(stockId);
        return getInvestorPath((countryCode)) + fileSeparator + stockId + fileSeparator + interval;
    }

    //매매동향 외국인기관
    public static String getInvestorPath(CountryCode countryCode){
        return getInvestorPath(countryCode.toString());
    }
    //매매동향 외국인기관
    public static String getInvestorPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.investor.dir.path", "investor");
    }

    public static String getProgramFilesPath(String stockId, String interval) {
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String countryCode = Stocks.getCountryCode(stockId);
        return getProgramPath((countryCode)) + fileSeparator + stockId + fileSeparator + interval;

    }

    public static String getProgramPath(CountryCode countryCode){
        return getProgramPath(countryCode.toString());
    }

    public static String getProgramPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.program.dir.path", "program");
    }

    public static String getVolumePowerFilesPath(String stockId, String interval){
        String fileSeparator = FileSystems.getDefault().getSeparator();
        String countryCode = Stocks.getCountryCode(stockId);
        return getVolumePowerPath((countryCode)) + fileSeparator + stockId + fileSeparator + interval;
    }

    public static String getVolumePowerPath(CountryCode countryCode){
        return getVolumePowerPath(countryCode.toString());
    }

    public static String getVolumePowerPath(String countryCode){
        return getSpotDirPath(countryCode, "stock.volume.power.dir.path", "volume_power");
    }


    public static String getSpotDirPath(String countryCode, String configKey, String dirName){
        String fileSeparator = FileSystems.getDefault().getSeparator();

        String dirPath = null;

        if(countryCode != null && !countryCode.isEmpty()) {
            dirPath = Config.getConfig(configKey + "." + countryCode);
            //대소문자 인식용
            if(dirPath == null){
                dirPath = Config.getConfig(configKey + "." + countryCode.toLowerCase());
            }
            if(dirPath == null){
                dirPath = Config.getConfig(configKey + "." + countryCode.toUpperCase());
            }
        }

        //국가별로 다르게 설정할 수 잇음
        //국내만 3천개의 종목이 넘고 미국은 만개의종목이 넘으므로 기본경로는 국가 코드가 들어가게 한다.
        if (dirPath == null) {

            if(countryCode == null){
                dirPath = TradingConfig.getTradingDataPath() + fileSeparator + "stock" + fileSeparator +"spot" + fileSeparator +dirName;
            }else{
                countryCode = countryCode.toUpperCase();
                dirPath = TradingConfig.getTradingDataPath() + fileSeparator + "stock" + fileSeparator  +countryCode + fileSeparator+"spot" + fileSeparator +dirName;
            }
        }

        return dirPath;
    }


    public static void main(String [] args){
        String path =getVolumePowerFilesPath("KOR_010130", "1d");
        System.out.println(path);
    }

}
