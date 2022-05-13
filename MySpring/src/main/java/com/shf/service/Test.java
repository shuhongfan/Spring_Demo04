package com.shf.service;

import com.shf.spring.MyApplicationContext;
import com.shf.spring.UserInterface;

public class Test {
    public static void main(String[] args) {
        MyApplicationContext applicationContext = new MyApplicationContext(AppConfig.class);

//        System.out.println(applicationContext.getBean("userService"));
//        System.out.println(applicationContext.getBean("userService"));
//        System.out.println(applicationContext.getBean("userService"));

        UserInterface userService = (UserInterface) applicationContext.getBean("userService");
        userService.test();
    }
}
