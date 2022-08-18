package com.iscas.aact.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.rpc.RPCController;
import com.iscas.aact.rpc.annotations.RPCHandler;
import com.iscas.aact.rpc.interfaces.IRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RPCHandler(name = "InitRecvHandler")
public class InitRecvHandler implements IRPCHandler {
    private static final Logger Log = LoggerFactory.getLogger(InitRecvHandler.class);

    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        // TODO: Verify rpc version
        if (!dataObj.getInteger("code").equals(IRPCHandler.CODE_SUCCESS)) {
            Log.error("Error when initializing rpc! received msg: {}", dataObj.toJSONString());
            return;
        }
        Log.info("RPC Initialize OK");
        controller.setReady(true);
    }
}
