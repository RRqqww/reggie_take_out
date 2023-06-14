package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登陆
     * @param employee
     * @param request
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(@RequestBody Employee employee, HttpServletRequest request){

        // 1.将页面提交的密码password进行MD5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());


        // 2.跟据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());   //lambda表达式，避免填错列名
        Employee emp = employeeService.getOne(queryWrapper); //因为数据库已经对这个字段作了唯一性约束，所以用getOne

        // 3.如果没有查询到则返回登陆失败结果
        if (emp == null){
            return R.error("登陆失败");
        }

        // 4.密码比对，如果不一致则返回登陆失败结果
        if (! emp.getPassword().equals(password)){
            return R.error("登陆失败");
        }

        // 5.查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0){
            return R.error("账号已禁用！");
        }
        // 6.登陆成功，将员工id存入session并返回登陆成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 退出登陆
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){      //这个地方data中存入的数据为字符串，所以泛型为String
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * 这个地方除了前端传过来的几个数据，期中status数据库中默认设置为1，不用传参，username数据库中也设置了唯一约束，
     * 新增重复的username会报错，设置了统一异常处理来处理这个重复username报错的问题
     * @param employee
     * @param request
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Employee employee,HttpServletRequest request){
        log.info("新增员工，员工信息为：{}，",employee);
        // 设置默认密码为123456，并采用MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        /*// 设置创建时间和更新时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //跟据session获取创建人(创建人就是登陆当前系统的人)的id
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);*/
        //存入数据库
        employeeService.save(employee);
        return R.success("添加员工成功");
    }


    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page,pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件（当我们没有输入name时，就相当于查询所有了）
        queryWrapper.like(!(name == null || "" == name),Employee::getName,name);
        //并对查询的结果进行降序排序，根据更新时间
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 因为前端只要一个code，所以返回值随意选了一个String
     * 前端的启用禁用以及编辑都是调用的这个update方法
     * @param employee
     * @param request
     * @return
     */
    @PutMapping()
    public R<String> update(@RequestBody Employee employee, HttpServletRequest request){

        log.info(employee.toString());
        /*Long id = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateUser(id);
        employee.setUpdateTime(LocalDateTime.now());*/
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");

    }


    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("跟据id查询员工信息");
        Employee employee = employeeService.getById(id);
        if (employee != null){
            return R.success(employee);
        }
        return R.error("未查询到对应员工信息");
    }


}
