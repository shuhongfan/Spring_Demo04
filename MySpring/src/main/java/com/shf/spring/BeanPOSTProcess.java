package com.shf.spring;

public interface BeanPOSTProcess {
    public Object postProcessBeforeInitialization(String beanName,Object bean);
    public Object postProcessAfterInitialization(String beanName,Object bean);
}
