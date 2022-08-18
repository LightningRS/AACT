package com.iscas.aact.testcase;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeConfigUtil {
    private static final Logger Log = LoggerFactory.getLogger(ScopeConfigUtil.class);
    protected final Map<String, Map<String, Boolean>> mFieldToScopeMap;

    public ScopeConfigUtil() {
        mFieldToScopeMap = new HashMap<>();
    }

    public boolean getScopeConfig(String fieldName, String scopeName) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.get(fieldName);
        if (scopeMap == null) return false;
        return scopeMap.get(scopeName);
    }

    protected void setScopeConfig(String fieldName, String scopeName, Boolean value) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.computeIfAbsent(fieldName, k -> new HashMap<>());
        scopeMap.put(scopeName, value);
    }

    public void loadScopeConfig(String pathOrContent, boolean isPath) {
        try (CSVReader csvReader = new CSVReader(isPath ? new FileReader(pathOrContent) : new StringReader(pathOrContent))) {
            String[] row = csvReader.readNext();
            List<String> scopeNames = new ArrayList<>();
            for (String scopeName : row) {
                if (!scopeName.equals("")) scopeNames.add(scopeName);
            }
            while ((row = csvReader.readNext()) != null) {
                if (row.length > 0) {
                    String fieldName = row[0];
                    for (int i = 1; i < row.length && i - 1 < scopeNames.size(); i++) {
                        setScopeConfig(fieldName, scopeNames.get(i - 1), row[i].equals("1"));
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            Log.error("Failed to read scope config csv file [{}]", pathOrContent, e);
        }
    }
}
