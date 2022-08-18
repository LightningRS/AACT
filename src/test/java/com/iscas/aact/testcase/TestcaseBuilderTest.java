package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.testcase.provider.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestcaseBuilderTest {
    @Test
    void testBuilder2() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel2.json").toAbsolutePath();
        Path outputCSV = Paths.get("src", "test", "resources", "test2.csv").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            JSONObject presetValues = new ValueProviderPreset().getValueSet();
            assertNotNull(presetValues);

            JSONObject compObj = compsArr.getJSONObject(39);
            System.out.println(compObj.getString("className"));
            TestcaseBuilder builder = new ACTSTestcaseBuilder(compObj);

//            JSONObject randValueSet = ValueProviderRandom.getRandomValueSet(5);
//            System.out.println(randValueSet.toJSONString());
//
//            JSONObject randWithStructValueSet = ValueProviderRandomWithStruct.getRandomWithStructValueSet(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig(), 5);
//            System.out.println(randWithStructValueSet.toJSONString());

            ValueProvider iccBotProvider = new ValueProviderICCBot(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig());
            builder.addValueSet(iccBotProvider.getValueSet());
            builder.addValueSet(presetValues);
            builder.build(outputCSV.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testBuilder() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel.json").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            JSONObject presetValues = new ValueProviderPreset().getValueSet();
            assertNotNull(presetValues);

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                Path outputCSV = Paths.get("src", "test", "resources", compObj.getString("className") + ".csv").toAbsolutePath();
                TestcaseBuilder builder = new ACTSTestcaseBuilder(compObj);

                JSONObject iccBotValueSet = new ValueProviderICCBot(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testBuilder3() {
        Path testModel = Paths.get("src", "test", "resources", "testComponentModel2.json").toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            JSONObject presetValues = new ValueProviderPreset().getValueSet();
            assertNotNull(presetValues);

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                Path outputCSV = Paths.get("src", "test", "resources", compObj.getString("className") + ".csv").toAbsolutePath();
                TestcaseBuilder builder = new ACTSTestcaseBuilder(compObj);

                JSONObject iccBotValueSet = new ValueProviderICCBot(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void test4strategies1() {
        buildWithAll4Strategies("testComponentModel.json");
        buildWithAll4Strategies("testComponentModel2.json");
    }

    void buildWithAll4Strategies(String modelName) {
        Path testModel = Paths.get("src", "test", "resources", modelName).toAbsolutePath();
        assertTrue(Files.exists(testModel));
        try {
            JSONObject modelObj = JSON.parseObject(Files.readString(testModel));
            JSONArray compsArr = modelObj.getJSONArray("components");

            JSONObject presetValues = new ValueProviderPreset().getValueSet();
            assertNotNull(presetValues);

            for (JSONObject compObj : compsArr.toJavaList(JSONObject.class)) {
                Path outputCSV = Paths.get("src", "test", "resources", "all4strategies", compObj.getString("className") + ".csv").toAbsolutePath();
                TestcaseBuilder builder = new ACTSTestcaseBuilder(compObj);

                JSONObject iccBotValueSet = new ValueProviderICCBot(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig()).getValueSet();
                JSONObject randValueSet = new ValueProviderRandom(5).getValueSet();
                JSONObject randWithStructValueSet = new ValueProviderRandomWithStruct(compObj.getJSONObject("fullValueSet"), builder.getScopeConfig(), 5).getValueSet();
                if (iccBotValueSet != null) builder.addValueSet(iccBotValueSet);
                if (randWithStructValueSet != null) builder.addValueSet(randWithStructValueSet);
                builder.addValueSet(randValueSet);
                builder.addValueSet(presetValues);
                builder.build(outputCSV.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
