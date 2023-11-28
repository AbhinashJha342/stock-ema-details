package com.local.ema.trade.details.parser;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class CSVParser {

    private static final Logger LOG = LoggerFactory.getLogger(CSVParser.class);

    public <T> List<T> parseFile(File file, Class<? extends T> targetClazz){

        try {
            CsvToBean<T> beans =
                    new CsvToBeanBuilder<T>(new FileReader(file))
                            .withType(targetClazz)
                            .build();
            return beans.parse();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
