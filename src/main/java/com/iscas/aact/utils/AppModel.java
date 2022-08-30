package com.iscas.aact.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class AppModel {
    private static final Logger Log = LoggerFactory.getLogger(AppModel.class);
    private final Path apkPath;
    private final ApkMeta apkMeta;
    private final Document apkManifest;
    private final JSONObject appModelJson;
    private final String pkgName;
    private final String modelVersion;
    private final SortedMap<String, JSONObject> componentsJsonMap;
    private final List<JSONObject> componentsJsonList;

    public AppModel(Path apkPath) throws IOException, SAXException, ParserConfigurationException {
        this.apkPath = apkPath;

        // Load ICCBot component model
        String apkFileName = apkPath.getFileName().toString();
        String apkFileBaseName = apkFileName.substring(0, apkFileName.lastIndexOf("."));
        Path appModelJsonPath = Paths.get(
                Config.getInstance().getIccResultPath().toString(),
                apkFileBaseName + "/ICCSpecification/ComponentModel.json");
        appModelJson = JSON.parseObject(Files.readString(appModelJsonPath));

        // Load APK
        try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
            apkMeta = apkFile.getApkMeta();
            pkgName = apkMeta.getPackageName();
            if (!pkgName.equals(appModelJson.getString("package"))) {
                throw new RuntimeException("APK package name not equals to component model package name!");
            }

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            apkManifest = docBuilder.parse(new InputSource(new StringReader(apkFile.getManifestXml())));
        }

        // Parse ICCBot component model
        modelVersion = appModelJson.getString("version");
        componentsJsonMap = new TreeMap<>();
        JSONArray compJsonArr = appModelJson.getJSONArray("components");
        if (compJsonArr == null) {
            Log.warn("[components] field not found in component model, package=" + pkgName + ", version=" + modelVersion);
        } else {
            List<JSONObject> compArr = compJsonArr.toJavaList(JSONObject.class);
            for (JSONObject comp : compArr) {
                componentsJsonMap.put(comp.getString("className"), comp);
            }
        }
        componentsJsonList = componentsJsonMap.values().stream().toList();

        log.info("Loaded APK [{}], with {} components in ICCBot component model", apkFileName, getCompCount());
    }

    public List<String> getAllCompNames() {
        return componentsJsonMap.keySet().stream().toList();
    }

    public String getPackageName() {
        return pkgName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public JSONObject getCompModelJsonByIndex(int index) {
        return componentsJsonList.get(index);
    }

    public CompModel getCompModelByIndex(int index) {
        JSONObject compJson = getCompModelJsonByIndex(index);
        if (compJson == null) {
            return null;
        }
        CompModel compModel = new CompModel(pkgName, compJson);
        ApkManifestUtils.checkCompEnabled(compModel, apkManifest);
        ApkManifestUtils.checkCompExported(compModel, apkManifest);
        MISTUtils.checkCompMISTType(compModel);
        return compModel;
    }

    public int getCompCount() {
        return componentsJsonMap.size();
    }
}
