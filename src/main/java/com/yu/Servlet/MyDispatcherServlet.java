package com.yu.Servlet;

import com.yu.annotation.MyController;
import com.yu.annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;


/**
 * MyDispatcherServlet，服务器启动时加载并初始化（init）
 * 请求发送到DispatcherServlet，被执行
 */

public class MyDispatcherServlet extends HttpServlet {

    //加载配置文件信息（扫描的包）
    private Properties properties = new Properties();

    //存入需要遍历的类名
    private List<String> classNames = new ArrayList<>();

    //IOC容器，放入控制器Controller类
    private Map<String, Object> ioc = new HashMap<>();

    //HandlerMapping模拟实现，
    private Map<String, Method> handlerMapping = new  HashMap<>();

    private Map<String, Object> controllerMap  =new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        //System.out.println(config.getInitParameter("contextConfigLocation"));
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        /**
         * 当servlet配置了初始化参数后，web容器在创建servlet实例对象时，
         * 会自动将这些初始化参数封装到ServletConfig对象
         */

        //2、扫描得到所有类
        doScanner(properties.getProperty("scanPackage"));

        //3、拿到扫描得到的类后，通过反射实例化，并放到IOC容器中
        doInstance();

        //4、初始化HandlerMapping（存对应的路径名和对应的方法）
        initHandlerMapping();

    }

    private void doLoadConfig(String location){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(is);    //将配置文件信息加载到Properties类中
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is){
                try {
                    is.close();     //加载完成后关闭InputStream流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //扫描包下所有的类，得到所有的类名
    private void doScanner(String packageName) {
        //扫描包使用的IO流，读进来的是文件路径，所以要把包路径改掉
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        //从该路径下获取文件（要加载的类）
        for (File file : dir.listFiles()){
            if (file.isDirectory()){
                //递归读取包，即使递归调用，但拼接的也只是最后一次递归得到的类名
                doScanner(packageName + "." + file.getName());
            }else{
                //是类
                String className = packageName + "." + file.getName().replace(".class","");
                classNames.add(className);
                System.out.println("扫描到的类有" + packageName + "." + file.getName());
            }
        }
    }

    //实例化控制器类
    private void doInstance(){
        if(classNames.isEmpty()){
            return;
        }
        for (String className : classNames){
            try {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MyController.class)){
                    //控制器类，放进ioc容器里
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()),clazz.newInstance());
                }else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String toLowerFirstWord(String name){
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    //HandlerMapping，储存的是请求地址对应的方法【xxx.do --> 对应方法】
    //故要获取的是RequestMapping地址
    private void initHandlerMapping(){
        if (ioc.isEmpty()){
            return;
        }

        try {
            //获取Map中的元素
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                //方法是放在控制器里的，我们需要的是控制器里的方法，所以跳过非控制器的RequestMapping
                if (!clazz.isAnnotationPresent(MyController.class)) {
                    continue;
                }

                //拼url时,是controller头的url拼上方法上的url
                String baseUrl = "";
                //获取类，当类上有MyRequestMapping注解时，是一个控制器类
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();   //URL地址为注解中的内容
                }
                //获取Controller类中的方法
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();
                    //在方法上的注解得到URL
                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, clazz.newInstance());
                    System.out.println(url + "," + method);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }






    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    //doDispatch的目的是执行方法（相当于HandlerAdapter转化和Controller执行两个过程）
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(handlerMapping.isEmpty()){
            return;
        }

        String url =req.getRequestURI();
        String contextPath = req.getContextPath();

        url=url.replace(contextPath, "").replaceAll("/+", "/");

        System.out.println(url);


        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        //执行一个方法，需要获取方法实体、参数列表
        //在HandlerMapping中获取对应的方法
        Method method =this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object [] paramValues= new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i<parameterTypes.length; i++){
            //根据参数名称，简化实现，只将request和response传过去
            String requestParam = parameterTypes[i].getSimpleName();

            if (requestParam.equals("HttpServletRequest")){
                //参数类型已明确，这边强转类型
                paramValues[i]=req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")){
                paramValues[i]=resp;
                continue;
            }
        }
        //利用反射机制来调用
        try {
            //Controller类中的方法实际上是在这里执行的
            method.invoke(this.controllerMap.get(url), paramValues);//第一个参数是method所对应的实例 在ioc容器中
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
