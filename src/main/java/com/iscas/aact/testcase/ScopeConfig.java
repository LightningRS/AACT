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

public class ScopeConfig {
    private static final Logger Log = LoggerFactory.getLogger(ScopeConfig.class);
    protected final Map<String, Map<String, Boolean>> mFieldToScopeMap;

    public ScopeConfig() {
        mFieldToScopeMap = new HashMap<>();
    }

    public ScopeConfig(ScopeConfig config) {
        this();
        for (Map.Entry<String, Map<String, Boolean>> entry : config.mFieldToScopeMap.entrySet()) {
            String fieldName = entry.getKey();
            Map<String, Boolean> fieldMap = entry.getValue();
            for (Map.Entry<String, Boolean> fieldEntry : fieldMap.entrySet()) {
                this.setFieldScopeConfig(fieldName, fieldEntry.getKey(), fieldEntry.getValue());
            }
        }
    }

    public boolean getScopeConfig(String fieldName, String scopeName) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.get(fieldName);
        if (scopeMap == null) {
            return false;
        }
        return scopeMap.get(scopeName);
    }

    public void setFieldScopeConfig(String fieldName, String scopeName, boolean value) {
        Map<String, Boolean> scopeMap = mFieldToScopeMap.computeIfAbsent(fieldName, k -> new HashMap<>());
        scopeMap.put(scopeName, value);
    }

    public void replaceFieldConfig(String fieldName, Boolean value) {
        if (mFieldToScopeMap.containsKey(fieldName)) {
            for (Map.Entry<String, Boolean> entry: mFieldToScopeMap.get(fieldName).entrySet()) {
                entry.setValue(value);
            }
        }
    }

    public void replaceScopeConfig(String fieldName, Boolean value) {
        for (Map.Entry<String, Map<String, Boolean>> entry : mFieldToScopeMap.entrySet()) {
            Map<String, Boolean> fieldMap = entry.getValue();
            if (fieldMap.containsKey(fieldName)) {
                fieldMap.put(fieldName, value);
            }
        }
    }

    public void loadScopeConfig(String pathOrContent, boolean isPath) {
        try (CSVReader csvReader = new CSVReader(isPath ? new FileReader(pathOrContent) : new StringReader(pathOrContent))) {
            String[] row = csvReader.readNext();
            List<String> scopeNames = new ArrayList<>();
            for (String scopeName : row) {
                if (!"".equals(scopeName)) {
                    scopeNames.add(scopeName);
                }
            }
            while ((row = csvReader.readNext()) != null) {
                if (row.length > 0) {
                    String fieldName = row[0];
                    for (int i = 1; i < row.length && i - 1 < scopeNames.size(); i++) {
                        setFieldScopeConfig(fieldName, scopeNames.get(i - 1), "1".equals(row[i]));
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            Log.error("Failed to read scope config csv file [{}]", pathOrContent, e);
        }
    }
}
