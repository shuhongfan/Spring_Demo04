package com.shf.service;

import com.shf.spring.*;


//@Scope("prototype")
@Component("userService")
public class UserService implements BeanNameAware, InitializeBean,UserInterface {
    @Autowired
    private OrderService orderService;

    private String beanName;

    public void test(){
        System.out.println(orderService);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName=beanName;
    }

    @Override
    public void afterPropertiesSet() {

    }
}
