package com.shf.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MyApplicationContext {
    private Class configClass;

    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
//    单例池
    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<String,Object>();

    private ArrayList<BeanPOSTProcess> beanPOSTProcessesList = new ArrayList<>();

    public MyApplicationContext(Class appConfigClass) {
        this.configClass = appConfigClass;

//        扫描
//        是否有ComponentScan注解
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
//            获取ComponentScan注解的value值
            ComponentScan configClassAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
//            扫描的包名
            String path = configClassAnnotation.value();  // com.shf.service
//            转换为扫描的路径
            path = path.replace(".","/"); // com/shf/service

            ClassLoader classLoader = MyApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());
//            System.out.println(file);

            if (file.isDirectory()){
                File[] files = file.listFiles();
                for (File f : files) {
                    String fileName = f.getAbsolutePath();
//                    System.out.println(fileName);

//                    筛选出后缀为.class文件
                    if (fileName.endsWith(".class")){
                        String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class")); //com\shf\service\UserService
//                        取出全类名
                        className = className.replace("\\", ".");
//                        System.out.println(className);

                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            // 是否有@component注解，有是bean，没有不是bean
                            if (clazz.isAnnotationPresent(Component.class)){
//                                是否是实现类
                                if (BeanPOSTProcess.class.isAssignableFrom(clazz)){
                                    Object instance = clazz.newInstance();
                                    beanPOSTProcessesList.add((BeanPOSTProcess) instance);
                                }

//                                获取bean的名字
                                Component component = clazz.getAnnotation(Component.class);
                                String beanName = component.value();

//                                定义一个bean
                                BeanDefinition beanDefinition = new BeanDefinition();
                                beanDefinition.setType(clazz);
                                //是否有@Scope注解，是否单例
                                if (clazz.isAnnotationPresent(Scope.class)){
                                    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                    beanDefinition.setScope(scopeAnnotation.value());
                                } else {
                                    beanDefinition.setScope("singleton");
                                }
                                beanDefinitionMap.put(beanName,beanDefinition);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
        }

//      创建单例bean对象
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                Object o = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName,o);
            }
        }
    }

    private Object createBean(String beanName,BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        try {
//            通过无参构造方法创建bean
            Object instance = clazz.getConstructor().newInstance();
//            依赖注入
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Autowired.class)){
                    f.setAccessible(true);
                    f.set(instance,getBean(f.getName()));
                }
            }

//        Aware回调    设置bean的名字
            if (instance instanceof BeanNameAware){
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            for (BeanPOSTProcess beanPOSTProcess : beanPOSTProcessesList) {
                instance = beanPOSTProcess.postProcessBeforeInitialization(beanName,instance);
            }

//            初始化
            if (instance instanceof InitializeBean){
                ((InitializeBean) instance).afterPropertiesSet();
            }

//            初始化后 AOP  BeanPOSTProcess
            for (BeanPOSTProcess beanPOSTProcess : beanPOSTProcessesList) {
                instance = beanPOSTProcess.postProcessAfterInitialization(beanName,instance);
            }

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition==null){
            throw new NullPointerException();
        } else {
            String scope = beanDefinition.getScope();
//            是单例还是多例的
            if (scope.equals("singleton")){
//                从单例池中取出
                Object bean = singletonObjects.get(beanName);
                if (bean==null){
//                    单例池中没有数据
                    Object o = createBean(beanName, beanDefinition);
                    singletonObjects.put(beanName,o);
                }
                return bean;
            } else {
//                创建多例bean
                return createBean(beanName,beanDefinition);
            }
        }
    }
}
