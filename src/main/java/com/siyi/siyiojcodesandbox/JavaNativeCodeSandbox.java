package com.siyi.siyiojcodesandbox;

import com.siyi.siyiojcodesandbox.model.ExecuteCodeRequest;
import com.siyi.siyiojcodesandbox.model.ExecuteCodeResponse;
import com.siyi.siyiojcodesandbox.model.ExecuteMessage;
import com.siyi.siyiojcodesandbox.model.enums.QuestionSubmitStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * @author siyi
 * @description: 本地代码沙箱
 * @date: 1/7/2024 8:46 下午
 */
@Component
@Slf4j
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        //1. 把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //        2. 编译代码，得到 class 文件
        ExecuteMessage compilerFileExecuteMessage = compilerFile(userCodeFile);
        if (compilerFileExecuteMessage.getExitValue()==1){
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            executeCodeResponse.setMessage(compilerFileExecuteMessage.getMessage());
            return executeCodeResponse;
        }
        //        3. 启动容器, 执行代码，得到输出结果
        List<ExecuteMessage> executeMessagesList = runFile(userCodeFile, inputList);
        // 遍历执行结果，如果有一个执行结果是失败的，就返回失败
        for (ExecuteMessage executeMessage : executeMessagesList) {
            if (executeMessage.getExitValue() == 1) {
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                executeCodeResponse.setMessage(executeMessage.getMessage());
                return executeCodeResponse;
            }
        }
        //        4. 收集整理输出结果
        executeCodeResponse = getOutputResponse(executeMessagesList);
        // 5.删除文件
        boolean b = clearFile(userCodeFile);
        if (!b) {
            log.error("删除文件失败" + userCodeFile.getAbsolutePath());
        }
        return executeCodeResponse;
    }
}
