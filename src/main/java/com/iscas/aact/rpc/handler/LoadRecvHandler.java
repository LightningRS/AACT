package com.iscas.aact.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.TestController;
import com.iscas.aact.rpc.RPCController;
import com.iscas.aact.rpc.annotations.RPCHandler;
import com.iscas.aact.rpc.interfaces.IRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RPCHandler(name = "LoadRecvHandler")
public class LoadRecvHandler implements IRPCHandler {
    private static final Logger Log = LoggerFactory.getLogger(LoadRecvHandler.class);

    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        int code = dataObj.getIntValue("code");
        if (code != IRPCHandler.CODE_SUCCESS) {
            Log.error("RPC returned error for ACTION_LOAD: {}", dataObj.toJSONString());
            controller.getTestController().setCurrCompState(TestController.STATE_ERROR);
            return;
        }
        int cnt = dataObj.getJSONObject("data").getIntValue("count");
        Log.info("RPC loaded {} testcases", cnt);
        controller.getTestController().setCurrCaseCount(cnt);
        controller.getTestController().setCurrCompState(TestController.STATE_LOADED_TESTCASE);
        controller.setLoaded(true);
    }
}
