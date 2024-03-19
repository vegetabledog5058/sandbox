package com.siyi.siyiojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.siyi.siyiojcodesandbox.model.ExecuteCodeRequest;
import com.siyi.siyiojcodesandbox.model.ExecuteCodeResponse;
import com.siyi.siyiojcodesandbox.model.ExecuteMessage;
import com.siyi.siyiojcodesandbox.model.JudgeInfo;
import com.siyi.siyiojcodesandbox.model.enums.JudgeInfoMessageEnum;
import com.siyi.siyiojcodesandbox.model.enums.QuestionSubmitStatusEnum;
import com.siyi.siyiojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Eric
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    private static final String SECURITY_MANAGER_PATH = "D:\\KAIFAMIAO\\IDEAworkspace\\siyi-code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        //1. 把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //        2. 编译代码，得到 class 文件
        ExecuteMessage compilerFileExecuteMessage = compilerFile(userCodeFile);
        System.out.println(compilerFileExecuteMessage);
//        3. 启动容器, 执行代码，得到输出结果

        List<ExecuteMessage> executeMessagesList = runFile(userCodeFile, inputList);


//        4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessagesList);


        // 5.删除文件
        boolean b = clearFile(userCodeFile);
        if (!b) {
            log.error("删除文件失败" + userCodeFile.getAbsolutePath());
        }
        // 6. 错误处理，提升程序健壮性
        return executeCodeResponse;
    }

    /**
     * 将用户代码保存为文件
     *
     * @param code 用户代码
     * @return File 代码文件
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        // 创建临时文件夹，例如：C:\code\siyioj-code-sandbox\tmpCode
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            // 不存在，则创建文件目录
            FileUtil.mkdir(globalCodePathName);
        }
        //将用户代码隔离存放
        String userCodePathName = globalCodePathName + File.separator + UUID.randomUUID();
        System.out.println("userCodePathName = " + userCodePathName);
        //实际存放用户代码的文件夹的java文件路径，例如：C:\code\siyioj-code-sandbox\tmpCode\1\SleepError.java
        String userCodePath = userCodePathName + File.separator + GLOBAL_JAVA_CLASS_NAME;
        System.out.println("userCodePath = " + userCodePath);
        //将用户代码写入文件
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译代码
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compilerFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");

            if (executeMessage.getExitValue() != 0) {
                log.info("编译错误");
                executeMessage.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
            }
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行文件获取结果列表
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        // 新增变量以跟踪最大内存使用
        long maxMemoryUsage = 0;
        String userCodePathName = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessagesList = new ArrayList<>();
        for (String inputArgs : inputList) {
            long beforeMemoryUsage = getCurrentMemoryUsage();

            String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodePathName, inputArgs);
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodePathName, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);

            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                long afterMemoryUsage = getCurrentMemoryUsage();
                long currentMemoryUsage = afterMemoryUsage - beforeMemoryUsage;
                maxMemoryUsage = Math.max(maxMemoryUsage, currentMemoryUsage);
                executeMessage.setMemory(maxMemoryUsage);
                executeMessagesList.add(executeMessage);
                System.out.println(executeMessage);
            } catch (Exception e) {
                log.error("运行错误", e);
            }
        }
        return executeMessagesList;
    }

    /**
     * 4. 获取响应输出结果
     *
     * @param executeMessagesList 执行结果
     * @return ExecuteCodeResponse 响应结果集合
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessagesList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessagesList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                executeCodeResponse.setMessage(errorMessage);

                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();

            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                memory=Math.max(memory,executeMessage.getMemory());
            }
        }
        //正常运行，也就是没有错误信息
        if (outputList.size() == executeMessagesList.size()) {
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setTime(maxTime/1024);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除执行后的文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean clearFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            log.info("删除" + (del ? "成功！" : "失败！"));
            return del;
        }
        return true;
    }

    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

}
