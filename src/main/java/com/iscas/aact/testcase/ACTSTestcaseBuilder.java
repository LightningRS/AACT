package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import com.opencsv.CSVWriter;
import edu.uta.cse.fireeye.common.*;
import edu.uta.cse.fireeye.service.engine.IpoEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ACTSTestcaseBuilder extends TestcaseBuilder {
    private static final Logger Log = LoggerFactory.getLogger(ACTSTestcaseBuilder.class);

    public ACTSTestcaseBuilder(JSONObject compInfo) {
        super(compInfo);
    }

    public ACTSTestcaseBuilder(JSONObject compInfo, ScopeConfigUtil scopeConfig) {
        super(compInfo, scopeConfig);
    }

    @Override
    public void build(String outputCSVPath) {
        if (!isValueSetModified) {
            Log.warn("No value collected, ignored generation");
            return;
        }
        // Flatten extra data
        new FlattenerExtra().flatten(mValueSet);
        new FlattenerCategory().flatten(mValueSet);

        SUT sut = new SUT(mCompInfo.getString("className"));

        // Add all parameters using sorted key set
        // head means that the parameter with the field name itself will be
        // placed at the head of the list.
        ArrayList<Parameter> catParams = new ArrayList<>();
        ArrayList<Parameter> extraParams = new ArrayList<>();
        for (String pName : new TreeSet<>(mValueSet.keySet())) {
            Parameter param = sut.addParam(pName);
            if (pName.startsWith("category_")) catParams.add(param);
            if (pName.startsWith("extra_")) extraParams.add(param);
            JSONArray values = mValueSet.getJSONArray(pName);
            for (String value : values.toJavaList(String.class)) {
                param.addValue(value);
            }
            param.setType(Parameter.PARAM_TYPE_ENUM);
        }

        // Build constraints for category
        if (catParams.size() > 0) {
            List<String> catConStrTrue = new ArrayList<>();
            List<String> catConStrFalse = new ArrayList<>();
            // Build condition statement
            catParams.forEach(c -> catConStrTrue.add(c.getName() + " = \"true\""));
            catParams.forEach(c -> catConStrFalse.add(c.getName() + " = \"false\""));
            // Append category itself
            ArrayList<Parameter> catParamsWithCat = new ArrayList<>(catParams);
            catParamsWithCat.add(sut.getParam("category"));
            // Build forward constraint
            Constraint catConForward = new Constraint("category = \"" + Constants.VAL_EMPTY + "\" => " +
                    String.join(" && ", catConStrFalse), catParamsWithCat);
            sut.addConstraint(catConForward);
            // Build reverse constraint
            Constraint catConReverse = new Constraint(String.join(" || ", catConStrTrue) + " => " +
                    "category = \"" + Constants.VAL_NOT_EMPTY + "\"", catParamsWithCat);
            sut.addConstraint(catConReverse);
        }

        // Build constraints for extra
        if (extraParams.size() > 0) {
            List<String> extraConStrNull = new ArrayList<>();
            List<String> extraConStrNotNull = new ArrayList<>();
            // Build condition statement
            extraParams.forEach(c -> extraConStrNull.add(c.getName() + " = \"" + Constants.VAL_NULL + "\""));
            extraParams.forEach(c -> extraConStrNotNull.add(c.getName() + " != \"" + Constants.VAL_NULL + "\""));
            // Append category itself
            ArrayList<Parameter> extraParamsWithExtra = new ArrayList<>(extraParams);
            extraParamsWithExtra.add(sut.getParam("extra"));
            // Build forward constraint
            Constraint extraConForward = new Constraint("extra = \"" + Constants.VAL_NULL + "\" || extra == \"" +
                    Constants.VAL_EMPTY + "\" => " + String.join(" && ", extraConStrNull), extraParamsWithExtra);
            sut.addConstraint(extraConForward);
            // Build reverse constraint
            Constraint extraConReverse = new Constraint(String.join(" || ", extraConStrNotNull) + " => " +
                    "extra = \"" + Constants.VAL_NOT_EMPTY + "\"", extraParamsWithExtra);
            sut.addConstraint(extraConReverse);

            // Build constraints for bundles
            for (Parameter extraParam : extraParams) {
                if (!extraParam.getName().endsWith("_bundle")) continue;
                String bdName = extraParam.getName();
                Pattern pattern = Pattern.compile("^extra_(?<parentId>\\d+)_(?<nodeId>\\d+)_" +
                        "(?<nodeName>[A-Za-z\\d]+)_(?<nodeType>[A-Za-z\\d_$.]+)$");
                Matcher matcher = pattern.matcher(bdName);
                if (!matcher.find() || !(matcher.groupCount() == 4))
                    throw new RuntimeException("Extra name pattern match failed: " + bdName);
                String nodeId = matcher.group("nodeId");
                ArrayList<Parameter> childParams = new ArrayList<>();
                List<String> childParamsConNull = new ArrayList<>();
                List<String> childParamsConNotNull = new ArrayList<>();
                for (Parameter param : extraParams) {
                    if (param.getName().startsWith("extra_" + nodeId + "_")) {
                        childParams.add(param);
                        childParamsConNull.add(param.getName() + " = \"" + Constants.VAL_NULL + "\"");
                        childParamsConNotNull.add(param.getName() + " != \"" + Constants.VAL_NULL + "\"");
                    }
                }
                if (childParams.size() == 0) continue;

                // child parameters relation
                // if (childParams.size() > 1) {
                //     Relation rChildParams = new Relation(Math.min(childParams.size(), 3));
                //     childParams.forEach(rChildParams::addParam);
                //     sut.addRelation(rChildParams);
                // }

                childParams.add(extraParam);
                Constraint fCon = new Constraint(bdName + " = \"" + Constants.VAL_NULL + "\" || " +
                        bdName + " = \"" + Constants.VAL_EMPTY + "\" => " +
                        String.join(" && ", childParamsConNull), childParams
                );
                Constraint rCon = new Constraint(String.join(" || ", childParamsConNotNull) + " => " +
                        bdName + " = \"" + Constants.VAL_NOT_EMPTY + "\"", childParams
                );
                sut.addConstraint(fCon);
                sut.addConstraint(rCon);
            }
        }

        // Build constraints for data
        ArrayList<Parameter> dataParams = new ArrayList<>(Arrays.asList(
                sut.getParam("data"),
                sut.getParam("scheme"),
                sut.getParam("authority"),
                sut.getParam("path")
        ));
        Constraint dataConForward = new Constraint("data = \"" + Constants.VAL_NULL + "\" || data = \"" +
                Constants.VAL_EMPTY + "\" => " + "scheme = \"" + Constants.VAL_EMPTY + "\" && authority = \"" +
                Constants.VAL_EMPTY + "\" && path = \"" + Constants.VAL_EMPTY + "\"", dataParams);
        sut.addConstraint(dataConForward);
        Constraint dataConReverse1 = new Constraint("scheme != \"" + Constants.VAL_EMPTY + "\" && " +
                "(authority != \"" + Constants.VAL_EMPTY + "\" || path != \"" + Constants.VAL_EMPTY + "\") => " +
                "data = \"" + Constants.VAL_NOT_EMPTY + "\"", dataParams);
        Constraint dataConReverse2 = new Constraint(
                "scheme = \"" + Constants.VAL_EMPTY + "\" => data = \"" + Constants.VAL_EMPTY + "\"",
                new ArrayList<>(Arrays.asList(sut.getParam("data"), sut.getParam("scheme")))
        );
        sut.addConstraint(dataConReverse1);
        sut.addConstraint(dataConReverse2);

        // Relation between basic fields
        Relation rAllFields = new Relation(3);
        rAllFields.addParam(sut.getParam("action"));
        rAllFields.addParam(sut.getParam("category"));
        rAllFields.addParam(sut.getParam("data"));
        rAllFields.addParam(sut.getParam("extra"));
        rAllFields.addParam(sut.getParam("type"));
        sut.addRelation(rAllFields);

        // Relation between category
        if (catParams.size() > 1) {
            Relation rCategory = new Relation(Math.min(catParams.size(), 2));
            catParams.forEach(rCategory::addParam);
            sut.addRelation(rCategory);
        }

        // Relation between extra
        if (extraParams.size() > 1) {
            Relation rExtra = new Relation(Math.min(extraParams.size(), 2));
            extraParams.forEach(rExtra::addParam);
            sut.addRelation(rExtra);
        }

        // Relation between data
        Relation rData = new Relation(2);
        rData.addParam(sut.getParam("scheme"));
        rData.addParam(sut.getParam("authority"));
        rData.addParam(sut.getParam("path"));
        sut.addRelation(rData);
        Log.info("Generated SUT:\n" + sut);

        sut.addDefaultRelation(2);

        // Generate
        TestGenProfile profile = TestGenProfile.instance();
        profile.setIgnoreConstraints(false);
        profile.setConstraintMode(TestGenProfile.ConstraintMode.solver);
        IpoEngine engine = new IpoEngine(sut);
        engine.build();
        TestSet ts = engine.getTestSet();
        // TestSetWrapper wrapper = new TestSetWrapper(ts, sut);
        // wrapper.outputInCSVFormat(outputCSVPath);

        // To CSV
        File file = new File(outputCSVPath);
        try {
            PrintWriter writer = new PrintWriter(file);
            int numOfTests = ts.getNumOfTests();
            int numOfParams = ts.getNumOfParams();
            CSVWriter csvWriter = new CSVWriter(writer);

            // Count full-combination size
            List<Integer> domainSizes = new ArrayList<>();
            for (int i = 0; i < numOfParams; i++) {
                domainSizes.add(sut.getParam(i).getDomainSize());
            }
            AtomicReference<BigInteger> fullCombSize = new AtomicReference<>(new BigInteger("1"));
            domainSizes.forEach(d -> fullCombSize.set(fullCombSize.get().multiply(new BigInteger(String.valueOf(d)))));

            Log.info("numOfParams = " + numOfParams + ", numOfTest = " + numOfTests);
            Log.info("fullCombSize = " + fullCombSize.get());
            List<String[]> comments = new ArrayList<>();
            comments.add(new String[]{"ACTS Test Suite Generation: " + new Date()});
            comments.add(new String[]{" '*' represents don't care value "});
            comments.add(new String[]{"Degree of interaction coverage: " + TestGenProfile.instance().getDOI()});
            comments.add(new String[]{"Number of parameters: " + ts.getNumOfParams()});
            comments.add(new String[]{"Maximum number of values per parameter: " + sut.getMaxDomainSize()});
            comments.add(new String[]{"Number of configurations: " + ts.getNumOfTests()});
            csvWriter.writeAll(comments);

            List<String> paramNames = sut.getParams().stream().map(Parameter::getName).toList();
            csvWriter.writeNext(paramNames.toArray(new String[0]));

            for (int i = 0; i < numOfTests; ++i) {
                List<String> values = new ArrayList<>();
                for (int j = 0; j < numOfParams; ++j) {
                    Parameter param = sut.getParam(j);
                    int col = ts.getColumnID(param.getID());
                    int value = ts.getValue(i, col);
                    if (value <= -10) {
                        int invalidValueIndex = -1 * (value + 10);
                        values.add(param.getInvalidValue(invalidValueIndex));
                    } else if (value != -1) {
                        if (param.getParamType() == 1) {
                            values.add(param.getValue(value));
                        } else {
                            values.add(param.getValue(value));
                        }
                    } else {
                        values.add("*");
                    }
                }
                csvWriter.writeNext(values.toArray(new String[0]));
            }
            csvWriter.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
