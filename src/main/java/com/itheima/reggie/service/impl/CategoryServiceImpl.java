package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomerException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;


    /**
     * 跟据id删除分类，删除之前需要进行判断
     * 这个方法最后面的super有点没搞懂
     * @param id
     */
    @Override
    public void remove(Long id) {
        // 添加dish的查询条件，根据分类id进行查询
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);

        int dishCount = dishService.count(dishLambdaQueryWrapper);
        // 查看当前分类是否关联菜品，如果已经关联，则抛出一个异常,,,,
        // 这个地方可以直接用R.error("当前分类下关联了菜品，不能删除")直接返回而不去自定义这个异常，都一样的功能，只是
        // 这个用异常去处理了   可能更优雅吧
        if (dishCount > 0){
            // 已关联菜品，抛出一个异常
            throw new CustomerException("当前分类下关联了菜品，不能删除");
        }


        // 添加setmeal的查询条件，根据分类id进行查询
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);

        int setmealCount = setmealService.count(setmealLambdaQueryWrapper);
        // 查看当前分类是否关联套餐，如果已经关联，则抛出一个异常
        if (setmealCount > 0){
            // 已关联套餐，抛出一个异常
            throw new CustomerException("当前分类下关联了套餐，不能删除");
        }
        // 正常删除
        super.removeById(id);


    }
}
