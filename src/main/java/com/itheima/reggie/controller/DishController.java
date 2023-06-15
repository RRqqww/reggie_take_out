package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {


    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("接收到的数据为：{}",dishDto);
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }


    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        // 构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        lambdaQueryWrapper.like(name != null,Dish::getName,name);
        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行分页查询
        dishService.page(pageInfo,lambdaQueryWrapper);

        // 对象拷贝
        //  除啦records属性不拷贝之外，其他的属性都拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        // 下面这一部分就是把records里面的CategoryId取出来，然后跟据CategoryId去查询分类名称，存入dishDto这个类
        // 这一部分可以用连表查询代替的
        List<Dish> records = pageInfo.getRecords();
        ArrayList<DishDto> dishDtoList = new ArrayList<>();

        for (Dish record : records) {

            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(record,dishDto);

            Long categoryId = record.getCategoryId();
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            dishDtoList.add(dishDto);
        }
        dishDtoPage.setRecords(dishDtoList);
        return R.success(dishDtoPage);

    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }


    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);

        // 清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        // 清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }


    /**
     * 根据菜品分类查询对应的菜品数据
     * 下面这个方法是在这个基础上进行了加强
     * @param dish
     * @return
     */
    //@GetMapping("/list")
    public R<List<Dish>> getA(Dish dish){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 跟据传进来的categoryId进行查询
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        // 只查询状态为1的菜品,,我感觉这里应该价格条件判断一下，否则这个SQL语句每次都会拼接上这个条件
        queryWrapper.eq(Dish::getStatus,1);
        // 对输出结果进行简单的排序
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        // 执行查询
        List<Dish> list = dishService.list(queryWrapper);
        // 返回结果
        return R.success(list);
    }


    /**
     * 根据菜品分类查询对应的菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> get(Dish dish){
        ArrayList<DishDto> dishDtoList = null;

        String key = "dish_" + dish.getCategoryId() + "_" +dish.getStatus();

        // 先从redis中获取缓存数据
        dishDtoList = (ArrayList<DishDto>) redisTemplate.opsForValue().get(key);

        // 如果存在直接返回，无需查询数据库
        if (dishDtoList != null){
            return R.success(dishDtoList);
        }



        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 跟据传进来的categoryId进行查询
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        // 只查询状态为1的菜品,,我感觉这里应该价格条件判断一下，否则这个SQL语句每次都会拼接上这个条件
        queryWrapper.eq(Dish::getStatus,1);
        // 对输出结果进行简单的排序
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        // 执行查询
        List<Dish> list = dishService.list(queryWrapper);
        // 返回结果


        // 这里需要new一个对象进行赋值，否则下面add的时候会空指针异常
        dishDtoList = new ArrayList<>();

        for (Dish dish1 : list) {

            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish1,dishDto);

            Long categoryId = dish1.getCategoryId();
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            // 这个地方仅需要这一部分，上面那个categoryname用不到
            //然后获取一下菜品id，根据菜品id去dishFlavor表中查询对应的口味，并赋值给dishDto
            Long dish1Id = dish1.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(dish1Id != null,DishFlavor::getDishId,dish1Id);
            //根据菜品id，查询到菜品口味
            List<DishFlavor> flavors = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            //赋给dishDto的对应属性
            dishDto.setFlavors(flavors);

            dishDtoList.add(dishDto);
        }
        // 如果不redis中存在，需要查询数据库，将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }
}
