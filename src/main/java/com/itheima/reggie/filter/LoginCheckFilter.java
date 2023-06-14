package com.itheima.reggie.filter;


import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    // 路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;


        //1.获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);

        //定义不需要处理的请求
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                //对用户登陆操作放行
                "/user/login",
                "/user/sendMsg"
        };
        //2.判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3.如果不需要处理，则直接放行
        if (check){
            log.info("本次请求：{}，不需要处理",requestURI);
            filterChain.doFilter(request,response);
            return;
        }

        //4.判断登录状态，如果已登录，则直接放行,,,这个是管理员页面
        if (request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，id：{}",request.getSession().getAttribute("employee"));

            //根据session来获取之前我们存的id值
            Long empId = (Long) request.getSession().getAttribute("employee");
            //使用basecontext封装我们存入session的id
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request,response);
            return;
        }


        //4.判断登录状态，如果已登录，则直接放行，，，这个是客户端
        if (request.getSession().getAttribute("user") != null){
            log.info("用户已登录，id：{}",request.getSession().getAttribute("user"));

            //根据session来获取之前我们存的id值
            Long userId = (Long) request.getSession().getAttribute("user");
            //使用basecontext封装我们存入session的id
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }

        //5.如果未登录则返回未登录结果,通过输出流方式向客户端页面响应数据
        log.info("用户未登录");
        // 暂时理解为这不是一个controller，不能直接返回json，用最原始的方法返回
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }


    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                // 匹配
                return true;
            }
        }
        // 不匹配
        return false;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
