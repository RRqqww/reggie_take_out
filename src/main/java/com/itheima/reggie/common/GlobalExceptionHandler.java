package com.itheima.reggie.common;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * @param ex
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());
        //如果包含Duplicate entry，则说明有条目重复
        if (ex.getMessage().contains("Duplicate entry")){
            //对字符串切片
            String[] split = ex.getMessage().split(" ");
            //字符串格式是固定的，所以这个位置必然是username
            String msg = split[2] + "已存在";
            //拼串作为错误信息返回
            return R.error(msg);
        }
        //如果是别的错误那我也没招儿了
        return R.error("位置错误");
    }


    @ExceptionHandler(CustomerException.class)
    public R<String> exceptionHandler(CustomerException ex){
        log.error(ex.getMessage());

        return R.error(ex.getMessage());
    }
}
