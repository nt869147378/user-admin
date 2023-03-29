package com.wasu.useradmin.entity;

import com.baomidou.mybatisplus.extension.api.R;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


//此类为通用的返回格式
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultData implements Serializable {
    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 0;
    public static final int ERROR_CODE = -1;

    public static final String SUCCESS_MSG = "成功";
    public static final String FAIL_MSG = "业务失败";
    public static final String ERROR_MSG = "系统异常";

    private Integer code;
    private String msg;
    private List<?> data;

    public static ResultData success(){
        return new ResultData(SUCCESS_CODE,SUCCESS_MSG,null);
    }

    public static ResultData success(String msg,List<?> data){
        return new ResultData(SUCCESS_CODE,msg,data);
    }

    public static Object success(String msg) {
        return new ResultData(SUCCESS_CODE,msg,null);
    }

    public static ResultData fail(){
        return new ResultData(FAIL_CODE,FAIL_MSG,null);
    }

    public static ResultData fail(String msg,List<?> data){
        return new ResultData(FAIL_CODE,msg,data);
    }

    public static ResultData fail(String s) {
        return new ResultData(FAIL_CODE,s,null);
    }

    public static ResultData error(){
        return new ResultData(ERROR_CODE,ERROR_MSG,null);
    }

    public static ResultData success(List<?> data) {
        return new ResultData(SUCCESS_CODE,null,data);
    }
}
