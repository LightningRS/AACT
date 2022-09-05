package com.iscas.aact.rpc.handler;

import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.rpc.RPCController;
import com.iscas.aact.rpc.annotations.RPCHandler;
import com.iscas.aact.rpc.interfaces.IRPCHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RPCHandler(name = "InitRecvHandler")
public class InitRecvHandler implements IRPCHandler {
    @Override
    public void handle(RPCController controller, JSONObject dataObj) {
        // TODO: Verify rpc version
        if (!dataObj.getInteger("code").equals(IRPCHandler.CODE_SUCCESS)) {
            log.error("Error when initializing rpc! received msg: {}", dataObj.toJSONString());
            return;
        }
        log.info("RPC Initialize OK");
        controller.setReady(true);
    }
}
