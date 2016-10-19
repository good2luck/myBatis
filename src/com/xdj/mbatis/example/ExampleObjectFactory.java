package com.xdj.mbatis.example;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import com.xdj.mbatis.model.User;

/**
 * @description 自定义结果集实例的ObjectFactory
 * @author xudj
 * @date 2016年9月8日 下午10:52:32
 * @version 1.0
 */
public class ExampleObjectFactory extends DefaultObjectFactory{
	private static final long serialVersionUID = 1L;

	public Object create(Class type) {  
        if(type.equals(User.class)){  
        	User obj= (User)super.create(type);  
            obj.setAge(1000);  
            obj.setNa_me("xudj_objectFactory");  
            return obj;  
        }  
        return super.create(type);  
    }  
  
    public void setProperties(Properties properties) {  
        Iterator iterator = properties.keySet().iterator();  
        while(iterator.hasNext()){  
            String keyValue = String.valueOf(iterator.next());  
            System.out.println(properties.getProperty(keyValue));  
        }  
        super.setProperties(properties);  
    }  
  
    public <T> boolean isCollection(Class<T> type) {  
        return Collection.class.isAssignableFrom(type);  
    }  
}
