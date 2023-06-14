package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.SendMsgUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 发送手机验证码
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpServletRequest request){
        //获取手机号
        String phone = user.getPhone();

        if (phone != null && phone != ""){
            //生成随机的四位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code = {}",code);

            // 调用阿里云提供的短信服务api完成发送短信
            //SMSUtils.sendMessage("阿里云短信测试","SMS_154950909",phone,code);
           // String code = SendMsgUtils.PhoneMsg(phone);

            // 需要将生成的验证码保存到session
            request.getSession().setAttribute(phone,code);
            return R.success("手机验证码短信发送成功");
        }
        return R.error("短信发送失败");

    }


    /**
     * 移动端用户登陆验证
     * @param map
     * @param request
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map,HttpServletRequest request){

        // 获取手机号
        String phone = (String) map.get("phone");
        // 获取验证码
        String loginCode = (String) map.get("code");
        // 从session中获取保存的验证码
        String sessionCode = (String) request.getSession().getAttribute(phone);

        // 将session中保存的验证码和用户提交的验证码进行比对
        if (sessionCode != null && sessionCode.equals(loginCode)){
             // 如果比对一样，登陆成功
            LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(lambdaQueryWrapper);
            // 判断当前登陆的手机号是否为新用户，如果是新用户，就自动完成注册
            if (user == null){
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            request.getSession().setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登陆失败");
    }
}
