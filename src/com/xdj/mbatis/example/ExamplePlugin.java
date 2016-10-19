package com.xdj.mbatis.example;

import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

/**
 * @description 拦截器示例类
 * @author xudj
 * @date 2016年9月7日 下午10:25:58
 * @version 1.0
 */
@Intercepts({@Signature(
		type= Executor.class,
		method = "update",
		args = {MappedStatement.class,Object.class})})
public class ExamplePlugin implements Interceptor {
	
	public Object intercept(Invocation invocation) throws Throwable {
		System.out.println("run intercept()");
		return invocation.proceed();
	}
	
	public Object plugin(Object target) {
		System.out.println("run plugin()");
		return Plugin.wrap(target, this);
	}
	
	public void setProperties(Properties properties) {
		System.out.println("run setProperties()");
	}
}
