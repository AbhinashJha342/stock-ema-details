package com.local.ema.trade.details.util;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.local.ema.trade.details.model.PriceParameters;
import com.local.ema.trade.details.model.StockPriceDetails;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StaticUtil {


    private static final CsvMapper mapper = new CsvMapper();
    /**
     * Method to read files from a directory. Meant to be used to bypass AWS connection issues.
     *
     * @param path The path from where we are reading the files
     * @return The list of files
     */
    public static List<File> getFilesAtPath(String path) {
        try {
            Path filesPath = Paths.get(path);
            if (filesPath.toFile().exists()) {
                try (Stream<Path> walk = Files.walk(filesPath)) {
                    List<File> newFiles =
                            walk.map(Path::toFile).filter(File::isFile).collect(Collectors.toList());
                    return newFiles;
                }
            }
        } catch (IOException e) {
            System.out.println("some exception "+e.getMessage());
        }
        return Collections.emptyList();
    }

    public static LocalDate parseToLocalDate(String str) throws DateTimeParseException {
        if (str == null) {
            throw new IllegalArgumentException("Date must not be null");
        }

        try {
            return LocalDate.parse(
                    str,
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (DateTimeParseException e) {
            return LocalDate.parse(
                    str,
                    DateTimeFormatter.ofPattern("M/d/yyyy"));
        }
    }

    public static PriceParameters createPriceParameters(StockPriceDetails priceDetails){
        return PriceParameters.builder()
                .date(priceDetails.getDate())
                .close(priceDetails.getClose())
                .dayRange(priceDetails.getDayRange())
                .openCloseRange(priceDetails.getOpenCloseRange())
                .emaDiffPercentage(priceDetails.getEmaDiffPercentage())
                .emaAppreciation(priceDetails.getEmaAppreciation())
                .highFastEmaPercentage(priceDetails.getHighFastEmaPercentage())
                .lowFastEmaPercentage(priceDetails.getLowFastEmaPercentage())
                .build();
    }

    public static File createCsvFile(List<PriceParameters> priceParameters, String fileName) {
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        JavaTimeModule javaTimeModule=new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE));
        mapper.registerModule(javaTimeModule);
        File file = new File(fileName);
        try (PrintWriter printWriter = new PrintWriter(file)) {
            // create header
            schemaBuilder.addColumn("date");
            schemaBuilder.addColumn("close");
            schemaBuilder.addColumn("dayRange");
            schemaBuilder.addColumn("openCloseRange");
            schemaBuilder.addColumn("emaDiffPercentage");
            schemaBuilder.addColumn("lowFastEmaPercentage");
            schemaBuilder.addColumn("highFastEmaPercentage");
            schemaBuilder.addColumn("emaAppreciation");
            schemaBuilder.addColumn("netPriceAppreciation");
            // header created

            CsvSchema schema =
                    schemaBuilder
                            .build()
                            .withLineSeparator(System.lineSeparator())
                            .withHeader();
            mapper.writer(schema).writeValues(printWriter).write(priceParameters);
        } catch (IOException e) {
            System.out.println("Exception!!"+ e.getMessage());
        }
        return file;
    }
}
