package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomerException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMpper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMpper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;


    /**
     * 用户结算购物车，支付
     * @param orders
     */
    @Override
    public void submit(Orders orders) {

        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //根据用户id查询其购物车数据
        shoppingCartLambdaQueryWrapper.eq(userId!= null,ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        // 判断购物车是否为空
        if (shoppingCartList == null){
            throw new CustomerException("购物车数据为空，不能进行下单");
        }
        // 判断地址是否有误
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null){
            throw new CustomerException("地址信息有误，不能下单");
        }
        // 获取用户信息，方便后面赋值
        User user = userService.getById(userId);
        long orderId = IdWorker.getId();
        AtomicInteger amount = new AtomicInteger(0);
        // 向订单细节表设置属性
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);   // 其实这个id可以不先生成，可以先执行orders表的逻辑，生成id，获取，然后放在这里
            orderDetail.setName(shoppingCart.getName());
            orderDetail.setImage(shoppingCart.getImage());
            orderDetail.setDishId(shoppingCart.getDishId());
            orderDetail.setSetmealId(shoppingCart.getSetmealId());
            orderDetail.setDishFlavor(shoppingCart.getDishFlavor());
            orderDetail.setNumber(shoppingCart.getNumber());
            orderDetail.setAmount(shoppingCart.getAmount());
            amount.addAndGet(shoppingCart.getAmount().multiply(new BigDecimal(shoppingCart.getNumber())).intValue());
            orderDetailList.add(orderDetail);
        }

        //向订单表设置属性
        orders.setId(orderId);
        orders.setNumber(String.valueOf(orderId));
        orders.setStatus(2);
        orders.setUserId(userId);
        orders.setAddressBookId(addressBookId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setAmount(new BigDecimal(amount.get()));
        orders.setPhone(addressBook.getPhone());
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(
                (addressBook.getProvinceName() == null ? "":addressBook.getProvinceName())+
                        (addressBook.getCityName() == null ? "":addressBook.getCityName())+
                        (addressBook.getDistrictName() == null ? "":addressBook.getDistrictName())+
                        (addressBook.getDetail() == null ? "":addressBook.getDetail())
        );

        //根据查询到的购物车数据，对订单表插入数据（1条）
        this.save(orders);
        //根据查询到的购物车数据，对订单明细表插入数据（多条）
        orderDetailService.saveBatch(orderDetailList);
        //清空购物车数据
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }
}
