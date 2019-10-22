package com.yu.controller;


import com.yu.annotation.MyController;
import com.yu.annotation.MyRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/springMVC")
public class TestController {

    @MyRequestMapping("/doTest1")
    public void test1(HttpServletRequest req, HttpServletResponse res){
//        System.out.println(param);
        try {
            res.getWriter().write( "doTest1 method success! param:"+ req.getParameter("param"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @MyRequestMapping("/doTest2")
    public void test2(HttpServletRequest req, HttpServletResponse res){
        try {
            res.getWriter().write( "doTest2 method success! param:"+ req.getParameter("param"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
