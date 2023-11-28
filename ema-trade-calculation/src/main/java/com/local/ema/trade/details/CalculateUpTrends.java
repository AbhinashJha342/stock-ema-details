package com.local.ema.trade.details;

import com.local.ema.trade.details.model.PriceParameters;
import com.local.ema.trade.details.model.StockPriceDetails;
import com.local.ema.trade.details.parser.CSVParser;
import com.local.ema.trade.details.util.StaticUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateUpTrends {

    private static final Logger LOG = LoggerFactory.getLogger(CalculateUpTrends.class);

    private static final MathContext mc = new MathContext(4, RoundingMode.HALF_UP);

    public static void main(String[] args) {
        System.out.println("starting!");
        CSVParser csvParser = new CSVParser();
        process(csvParser);
    }

    /**
     * reads the incoming csv file with stock details and creates an object.
     *
     * @param csvParser
     */
    private static void process(CSVParser csvParser){
        File file = StaticUtil.getFilesAtPath("src/main/resources/IXIC_22_23_simplified.csv").get(0);
        List<StockPriceDetails> stockPriceDetails = csvParser.parseFile(file, StockPriceDetails.class);
        LOG.info("number of rows parsed {}", stockPriceDetails.size());

        List<PriceParameters> priceParameters = findUptrendAndGetPriceMovementDetails(stockPriceDetails);

        StaticUtil.createCsvFile(priceParameters, file.getName());
    }

    /**
     * reads the list of object with stock price details. checks for the uptrend using following rules : <br>
     * 1. low - fast ema % > 0.
     * 2. keeps a counter(trendInterruptedCounter) of 2 days when low - fast ema % < 0.
     * 3. maintains a indexWhereTrendStarted counter to keep track of the day when first the above 2 conditions met.
     * 4. till above 2 condition satisfies, keeps creating object with some stock price details and adds to another list.
     * 5. once the buffer of 2 days exceeds, then <br>
     *      a. this day marks the end of trend.
     *      b. calculate the close-close %age movement of the stock price during these days.
     *      c. creates a map using the value of (b.) as key, and the list of objects in the range from indexWhereTrendStarted to the day trend breaks.
     * 6. resets the trendInterruptedCounter to 0.
     */
    private static List<PriceParameters> findUptrendAndGetPriceMovementDetails(List<StockPriceDetails> stockPriceDetails){
        List<PriceParameters> priceParametersList = new ArrayList<>();
        Map<BigDecimal, String> mapOfPriceParametersWithNetMvmtInUptrend = new HashMap<>();
        int indexWhereTrendStarted = 0;
        boolean doesTrendExists = false;
        boolean isStartAlreadySet = false;
        int trendInterruptedCounter = 0;
        int lastIndexWhenLowWasAboveFastEma = 0;

        for (StockPriceDetails priceDetails : stockPriceDetails) {
            if (priceDetails.getLowFastEmaPercentage().compareTo(BigDecimal.ZERO) > 0) {
                doesTrendExists = true;
                PriceParameters parameters = StaticUtil.createPriceParameters(priceDetails);
                priceParametersList.add(parameters);
                trendInterruptedCounter = 0;
                if (!isStartAlreadySet) {
                    indexWhereTrendStarted = priceParametersList.size() - 1;
                }
                isStartAlreadySet = true;
                lastIndexWhenLowWasAboveFastEma = priceParametersList.size() - 1;
            } else if (priceDetails.getLowFastEmaPercentage().compareTo(BigDecimal.ZERO) <= 0 && trendInterruptedCounter < 2 && doesTrendExists) {
                PriceParameters parameters = StaticUtil.createPriceParameters(priceDetails);
                priceParametersList.add(parameters);
                ++trendInterruptedCounter;
            } else if (trendInterruptedCounter == 2) {
                doesTrendExists = false;
                isStartAlreadySet = false;
                BigDecimal closeWhereTrendStarted = priceParametersList.get(indexWhereTrendStarted).getClose();
                BigDecimal closeWhereTrendEnds = priceParametersList.get(lastIndexWhenLowWasAboveFastEma).getClose();
                BigDecimal priceAppreciationDuringUptrend = (closeWhereTrendEnds.subtract(closeWhereTrendStarted)).divide(closeWhereTrendStarted, mc);
                mapOfPriceParametersWithNetMvmtInUptrend.put(priceAppreciationDuringUptrend.multiply(BigDecimal.valueOf(100)), indexWhereTrendStarted+"-"+lastIndexWhenLowWasAboveFastEma);
                trendInterruptedCounter = 0;
            }

        }

        if(doesTrendExists){
            BigDecimal closeWhereTrendStarted = priceParametersList.get(indexWhereTrendStarted).getClose();
            BigDecimal closeWhereTrendEnds = priceParametersList.get(lastIndexWhenLowWasAboveFastEma).getClose();
            BigDecimal priceAppreciationDuringUptrend = (closeWhereTrendEnds.subtract(closeWhereTrendStarted)).divide(closeWhereTrendStarted, mc);
            mapOfPriceParametersWithNetMvmtInUptrend.put(priceAppreciationDuringUptrend.multiply(BigDecimal.valueOf(100)), indexWhereTrendStarted+"-"+lastIndexWhenLowWasAboveFastEma);
        }

        return retrievePriceHistoryOfUptrends(mapOfPriceParametersWithNetMvmtInUptrend, priceParametersList);
    }

    private static List<PriceParameters> retrievePriceHistoryOfUptrends(Map<BigDecimal, String> mapOfPriceParametersWithNetMvmtInUptrend, List<PriceParameters> priceParametersList){
        List<PriceParameters> filteredPriceParameters = new ArrayList<>();
        for(Map.Entry<BigDecimal, String> priceMovementDetails : mapOfPriceParametersWithNetMvmtInUptrend.entrySet()) {
            String[] indexes = priceMovementDetails.getValue().split("-");
            BigDecimal priceAppreciation = priceMovementDetails.getKey();
            int startIndex = Integer.parseInt(indexes[0]);
            int endIndex = Integer.parseInt(indexes[1]);
            PriceParameters intermediateTrendEnd = priceParametersList.get(endIndex);
            intermediateTrendEnd.setNetPriceAppreciation(priceAppreciation);
            int counter = startIndex;
            while(counter<=endIndex) {
                if(counter == endIndex) {
                    filteredPriceParameters.add(intermediateTrendEnd);
                    counter++;
                    continue;
                }
                filteredPriceParameters.add(priceParametersList.get(counter));
                counter++;
            }
        }

        return filteredPriceParameters;
    }
}
