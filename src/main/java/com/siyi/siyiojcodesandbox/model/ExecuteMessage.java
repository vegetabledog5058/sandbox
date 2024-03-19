package com.siyi.siyiojcodesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 * @author siyi
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}
