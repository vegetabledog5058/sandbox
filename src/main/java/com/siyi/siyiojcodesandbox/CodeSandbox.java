package com.siyi.siyiojcodesandbox;

import com.siyi.siyiojcodesandbox.model.ExecuteCodeRequest;
import com.siyi.siyiojcodesandbox.model.ExecuteCodeResponse;

/**
 * @author: siyi
 * @description: 代码沙箱接口定义
 * @date: 1/7/2024 8:44 下午
 */
public interface CodeSandbox {
    /**
     * 执行代码
     *
     * @param executeCodeRequest 请求参数
     * @return 执行结果
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
