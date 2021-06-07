package service;

import lombok.extern.log4j.Log4j2;
import model.InputModel;
import model.InputRow;
import model.OutputRow;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Log4j2
@Service
public class FileService {

    public static List<OutputRow> sortFile(Path file) {
        log.info("Sorting file *** {} ***", file.getFileName());

        List<InputRow> data = getData(file);
        String chosenMetric = "net_sales";
        return hierarchicalSort(data, chosenMetric);
    }

    private static List<OutputRow> hierarchicalSort(List<InputRow> data, String metric) {
        //sort by category
        data.sort(Comparator.comparing(inputRow -> inputRow.getProperties().get(0)));
        List<List<InputRow>> inputRowsByProperty = collectInputRowsByProperty(data);

        int metricIndex = getMetricIndex(data.get(0), metric);
        List<InputRow> inputRowsWithTotalOnTop = new ArrayList<>();
        for (int i = 0; i < inputRowsByProperty.size(); i++) {
            //sort subcategories by chosen metric
            inputRowsByProperty.get(i).sort(Comparator.comparing(inputRow -> inputRow.getProperties().get(1)));
            List<List<InputRow>> inputRowsBySubProperty = collectInputRowsBySubproperty(inputRowsByProperty.get(i), metricIndex);
            for (int j = 0; j < inputRowsBySubProperty.size(); j++) {
                inputRowsWithTotalOnTop.addAll(placeTotalOnTopOfList(inputRowsBySubProperty.get(j)));
            }
        }
        return mapToOutput(inputRowsWithTotalOnTop);
    }

    private static List<OutputRow> mapToOutput(List<InputRow> inputRowsWithTotalOnTop) {
        return inputRowsWithTotalOnTop.stream().map(inputRow -> {
            return OutputRow.builder().data(mapDataFromInputRow(inputRow)).build();
        }).collect(Collectors.toList());
    }

    private static int getMetricIndex(InputRow inputRow, String metric) {
        for (int i=0; i< inputRow.getMetrics().size(); i++) {
            if (inputRow.getMetrics().get(i).equals(metric)) {
                return i;
            }
        }
        return 0;
    }

    private static List<InputRow> placeTotalOnTopOfList(List<InputRow> inputRows) {
        List<InputRow> inputsWithTotalOnTop = new ArrayList<>();
        int subCategoryIndex = 0;
        String subCategory = inputRows.get(0).getProperties().get(1);
        for (InputRow inputRow : inputRows) {
            if (!inputRow.getProperties().get(1).equals(subCategory)) {
                subCategoryIndex++;
                subCategory = inputRow.getProperties().get(1);
            }
            if (inputRow.getProperties().get(1).equals("$total") && inputRow.getProperties().get(2).equals("$total")) {
                inputsWithTotalOnTop.add(0, inputRow);
            } else if (inputRow.getProperties().get(2).equals("$total")) {
                inputsWithTotalOnTop.add(subCategoryIndex, inputRow);
                subCategoryIndex++;
            } else {
                inputsWithTotalOnTop.add(inputRow);
            }
        }
        return inputsWithTotalOnTop;
    }

    private static List<List<InputRow>> collectInputRowsByProperty(List<InputRow> data) {
        List<List<InputRow>> inputRowsByCategory = new ArrayList<>();
        int categoryBeginIndex = 0;
        int categoryEndIndex = 0;
        String categoryName = data.get(0).getProperties().get(0);
        for (InputRow row : data) {
            if (categoryName.equals(row.getProperties().get(0))) {
                categoryEndIndex++;
            } else {
                inputRowsByCategory.add(data.subList(categoryBeginIndex, categoryEndIndex));
                categoryBeginIndex = categoryEndIndex;
                categoryEndIndex++;
                categoryName = row.getProperties().get(0);
            }
        }
        for (int i = 0; i < inputRowsByCategory.size(); i++) {
            inputRowsByCategory.get(i).sort(Comparator.comparing(inputRow -> inputRow.getProperties().get(1)));
        }
        return inputRowsByCategory;
    }

    private static List<List<InputRow>> collectInputRowsBySubproperty(List<InputRow> inputRows, int metricIndex) {
        List<List<InputRow>> inputRowsBySubcategory = new ArrayList<>();
        int subcategoryBeginIndex = 0;
        int subcategoryEndIndex = 0;
        String subcategoryName = inputRows.get(0).getProperties().get(1);
        for (InputRow row : inputRows) {
            if (subcategoryName.equals(row.getProperties().get(1))) {
                subcategoryEndIndex++;
            } else {
                inputRowsBySubcategory.add(inputRows.subList(subcategoryBeginIndex, subcategoryEndIndex));
                subcategoryBeginIndex = subcategoryEndIndex;
                subcategoryEndIndex++;
                subcategoryName = row.getProperties().get(1);
            }
        }
        for (int i = 0; i < inputRowsBySubcategory.size(); i++) {
            inputRowsBySubcategory.get(i).sort(Comparator.comparing(inputRow -> inputRow.getMetrics().get(metricIndex))); //sort by double value
        }
        return inputRowsBySubcategory;
    }

    private static String mapDataFromInputRow(InputRow inputRow) {
        StringBuilder sb = new StringBuilder();
        for (String property : inputRow.getProperties()) {
            sb.append(property);
            sb.append("|");
        }
        for (String metric : inputRow.getMetrics()) {
            sb.append(metric);
            sb.append("|");
        }
        return sb.toString();
    }

    private static List<InputRow> getData(Path file) {
        List<InputRow> inputRows = new ArrayList<>();
        try {
            List<String> rows = collectRows(file);
            InputModel exampleInputSize = mapPropertiesAndMetricsQuantities(rows.get(0));
            rows.forEach(row -> {
                String[] values = row.split("\\|");
                InputRow inputRow = getPropertiesAndMetricValues(values, exampleInputSize);
                inputRows.add(inputRow);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputRows;
    }

    private static InputRow getPropertiesAndMetricValues(String[] values, InputModel exampleInputSize) {
        List<String> properties = new ArrayList<>();
        List<String> metrics = new ArrayList<>();
        for (int i = 0; i < exampleInputSize.getPropertyQuantity(); i++) {
            properties.add(values[i]);
        }
        for (int i = exampleInputSize.getPropertyQuantity(); i < (exampleInputSize.getMetricsQuantity() + exampleInputSize.getPropertyQuantity()); i++) {
            metrics.add(values[i]);
        }
        return InputRow.builder().properties(properties).metrics(metrics).build();
    }

    private static List<String> collectRows(Path file) throws IOException {
        return new BufferedReader(new FileReader(file.toFile())).lines().collect(Collectors.toList());
    }

    private static InputModel mapPropertiesAndMetricsQuantities(String header) {
        List<String> properties = new ArrayList<>();
        List<String> metrics = new ArrayList<>();

        String[] headerTokens = header.split("\\|");
        for (String headerToken : headerTokens) {
            if (headerToken.contains("property")) {
                properties.add(headerToken);
            } else {
                metrics.add(headerToken);
            }
        }
        log.info("{} properties and {} metrics found...", properties.size(), metrics.size());
        return InputModel.builder().propertyQuantity(properties.size()).metricsQuantity(metrics.size()).build();
    }
}
