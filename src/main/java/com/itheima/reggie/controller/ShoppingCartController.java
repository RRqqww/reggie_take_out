package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    /**
     * 添加商品到购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车添加信息：{}",shoppingCart);

        // 获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        // 设置当前用户id
        shoppingCart.setUserId(currentId);
        // 获取当前菜品id
        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        // 判断添加的是菜品还是套餐
        if (dishId != null){
            // 添加的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            // 添加的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        // 查询当前菜品或者套餐是否在购物车中
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null){
            // 当前菜品或者套餐在购物车中，在当前的数量上+1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else {
            // 如果不存在还需要设置一下创建时间，这个不能使用自动填充，因为自动填充中insertFill里面需要4个字段，这里只有一个，会报错
            shoppingCart.setCreateTime(LocalDateTime.now());
            // 当前菜品或者套餐不在购物车中，添加到购物车，数量默认为1
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            //这里是为了统一结果，最后都返回cartServiceOne会比较方便
            cartServiceOne = shoppingCart;
        }
        return R.success(cartServiceOne);

    }


    /**
     * 获取购物车内商品的集合
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        Long userId = BaseContext.getCurrentId();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        return R.success(shoppingCarts);
    }


    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean() {
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        queryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);
        //删除当前用户id的所有购物车数据
        shoppingCartService.remove(queryWrapper);
        return R.success("成功清空购物车");
    }
}
