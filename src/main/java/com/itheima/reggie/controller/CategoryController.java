package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 这个类新增的时候 实体类有is_delete字段不报错，分页查询时就开始报错了
 */
@RestController
@Slf4j
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    @PostMapping
    public R<String> save(@RequestBody Category category){
        log.info("category:{}", category);
        boolean save = categoryService.save(category);
        if (save){
            return R.success("新增分类成功");
        }
        return R.error("新增分类失败");
    }

    @GetMapping("/page")
    public R<Page> page(int page,int pageSize){
        // 分页构造器
        Page<Category> pageInfo = new Page<>(page,pageSize);
        // 条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加排序条件，跟据sort进行排序
        queryWrapper.orderByDesc(Category::getSort);
        // 进行分页查询
        categoryService.page(pageInfo,queryWrapper);
        // 这个地方应该跟据查询的page对象 判断其中的某个属性（具体忘了），看是否查看成功，成功直接返回成功，失败返回失败信息
        return R.success(pageInfo);
    }


    @DeleteMapping
    private R<String> delete(Long id){
        log.info("将被删除的id：{}",id);
        //categoryService.removeById(id);
        categoryService.remove(id);
        return R.success("分类信息删除成功");
    }


    @PutMapping()
    public R<String> update(@RequestBody Category category){
        log.info("修改分类信息为：{}",category);
        categoryService.updateById(category);
        return R.success("修改分类信息成功");
    }


    /**
     * 获取菜品分类数据并展示到下拉框中
     * 这个地方的参数可以是Category，也可以是type，因为前端传的参数是type，Category这个类中包含type属性，优雅
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        //条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件，这里只需要判断是否为菜品（type为1是菜品，type为2是套餐）
        lambdaQueryWrapper.eq(category.getType() != null,Category::getType,category.getType());
        // 添加排序条件
        lambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        // 查询数据
        List<Category> list = categoryService.list(lambdaQueryWrapper);
        // 返回数据
        return R.success(list);
    }
}
