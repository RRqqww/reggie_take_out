package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomerException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐
     * @param setmealDto
     */
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        // 直接保存套餐
        this.save(setmealDto);
        // 获取套餐中的dish
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        // 为套餐中的每个dish添加其所属的套餐id
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDto.getId());
        }

        // 批量保存每个套餐的dish
        setmealDishService.saveBatch(setmealDishes);

    }

    /**
     * 删除套餐
     * 这个地方的逻辑是有问题的，因为批量删除时，一旦批量所选中套餐中有一个再售不能删除，则所有的都不能删除
     * 下面的批量删除的逻辑应该遍历一个一个执行，不用in，
     * @param ids
     */
    @Override
    public void removeWithDish(List<Long> ids) {

        // 先判断一下能不能删除，如果状态为1，则是再售套餐，不能删除
        //select * from setmeal where id in (ids) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);
        int count = this.count(queryWrapper);
        //
        if (count > 0){
            throw new CustomerException("套餐正在售卖中，请先停售在进行删除");
        }
        // 如果没有状态为1的在售套餐，则就直接删除
        this.removeByIds(ids);
        // 继续删除套餐所关联的菜品信息，，，
        // 这个地方先删除套餐所关联的所有菜品信息，再删除套餐，我感觉这样更合理一些
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);

    }


    /**
     * 自己写的有问题，，批量删除，能局部删除成功，但是页面没有进行分页查询，页面没刷新
     * 还有就是，抛出异常之后，套餐所关联的菜品信息没有删除
     * @param ids
     */
    public void removeWithDishA(List<Long> ids) {
        // 先判断一下能不能删除，如果状态为1，则是在售套餐，不能删除
        //select * from setmeal where id in (ids) and status = 1


        ArrayList<Long> arrayList = new ArrayList<>();
        for (Long id : ids) {
            LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Setmeal::getId,id);
            queryWrapper.eq(Setmeal::getStatus,1);

            int count = this.count(queryWrapper);
            //
            if (count > 0) {
                arrayList.add(id);
            }else {
                // 如果没有状态为1的在售套餐，则就直接删除
                this.removeById(id);
            }


        }

        if (arrayList.size() > 0){
            // 这个地方传给前端的是id，应该跟据ID查询名称拼成字符串返回给前端
            throw new CustomerException("所选中的套餐"+arrayList.toString()+"正在售卖中，请先停售在进行删除");
        }


        // 继续删除套餐所关联的菜品信息，，，
        // 这个地方先删除套餐所关联的所有菜品信息，再删除套餐，我感觉这样更合理一些
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
    }
}
