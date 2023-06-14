package com.itheima.reggie.controller;


import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 这个方法的参数想要自动接收的话必须是file（因为前端表单传参的时候用的是name="file"）,要不就是requestparam指定一下
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        // file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
        log.info("获取文件：{}",file.toString());

        // 获取传入的原文件名
        String originalFilename = file.getOriginalFilename();
        // 获取传入文件格式的后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")); // .jpg
        // 为了防止文件名重复，使用uuid进行拼接
        String fileName = UUID.randomUUID() + suffix;

        // 判断一下目录是否存在，不存在则创建,后面file.transferTo转存文件的时候，如果文件夹不存在，则会报错
        // 所以我们得先创建好文件夹
        File dir = new File(basePath);
        if (!dir.exists()){
            dir.mkdirs();
        }

        // 将其转存到我们指定的文件夹
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将文件名返回给前端，便于后面前端进行文件下载，然后显示在页面上
        return R.success(fileName);

    }


    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){
        FileInputStream fileInputStream = null;
        ServletOutputStream outputStream = null;


        try {
            File dir = new File(basePath+name);
            if (dir.exists()){
                // 输入流，通过输入流读取文件内容
                fileInputStream = new FileInputStream(new File(basePath + name));
                // 输出流，通过输出流将文件写回到浏览器进行显示
                outputStream = response.getOutputStream();

                response.setContentType("image/jpeg");

                int len = 0;
                byte[] bytes = new byte[1024];
                while ((len = fileInputStream.read(bytes)) != -1){
                    outputStream.write(bytes,0,len);
                    outputStream.flush();
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }




    }
}
