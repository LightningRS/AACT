package com.iscas.aact.logcat.handler;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;
import com.iscas.aact.logcat.utils.TraceBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@LogcatHandler(name = "StackTraceHandler", regex = ": (\\t|Caused by:).*")
public class StackTraceHandler implements ILogcatHandler {
    private LogInfo mLastLineInfo = null;
    private LogInfo mTraceHeadInfo = null;
    private final List<String> mTraceBlock = new ArrayList<>();
    private final Queue<LogInfo> mTraceHeadInfoQueue = new ConcurrentLinkedQueue<>();
    private final Queue<String> mTraceBlockQueue = new ConcurrentLinkedQueue<>();

    public void setLastLineInfo(LogInfo logInfo) {
        if (logInfo != mLastLineInfo) {
            // Log block end
            if (mTraceBlock.size() > 0) {
                // Put trace block into queue and reset the buffer
                mTraceHeadInfoQueue.offer(mTraceHeadInfo);
                mTraceBlockQueue.offer(String.join("\n", mTraceBlock));
                mTraceBlock.clear();
                mTraceHeadInfo = null;
            }
        }
        mLastLineInfo = logInfo;
    }

    public boolean hasTraceBlock() {
        assert mTraceHeadInfoQueue.size() == mTraceBlockQueue.size();
        return mTraceHeadInfoQueue.size() > 0;
    }

    public TraceBlock getTraceBlock() {
        TraceBlock res = new TraceBlock();
        assert mTraceHeadInfoQueue.size() == mTraceBlockQueue.size();
        res.headInfo = mTraceHeadInfoQueue.poll();
        res.body = mTraceBlockQueue.poll();
        if (res.headInfo == null || res.body == null) return null;
        return res;
    }

    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        if (logInfo.msg == null) return;
        if (logInfo.msg.startsWith("Caused by:")) {
            if (mLastLineInfo == null || !mLastLineInfo.msg.startsWith("\tat")) return;
        } else if (!logInfo.msg.startsWith("\tat")) {
            return;
        }
        if (mTraceBlock.size() < 1 && mLastLineInfo != null) {
            // Trace block start, mLastLineInfo is the first line
            mTraceBlock.add(mLastLineInfo.level + " " + mLastLineInfo.tag + ": " + mLastLineInfo.msg);
            mTraceHeadInfo = mLastLineInfo;
        }
        mTraceBlock.add(logInfo.level + " " + logInfo.tag + ": " + logInfo.msg);
        mLastLineInfo = logInfo;
    }
}
