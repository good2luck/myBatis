package com.xdj.mbatis.test;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import com.xdj.mbatis.mapper.UserMapper;
import com.xdj.mbatis.model.User;

/**
 * @description 
 * @author xudj
 * @date 2016年4月21日 下午1:08:13
 * @version 1.0
 */
public class TestMain {

	private static SqlSessionFactory sqlSessionFactory;
	
	static{
		
	}
	
	public static void main(String[] args) {
		String reourse = "mybatis-config.xml"; 
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		try {
			sqlSessionFactory = builder.build(Resources.getResourceAsStream(reourse));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		SqlSession sqlSession = null;
		try{
			sqlSession = sqlSessionFactory.openSession();
			UserMapper userMapper = null;
			userMapper = sqlSession.getMapper(UserMapper.class);
			User user = userMapper.getUserById(1);
			System.out.println(user);
			
//			List<Integer> list = userMapper.testGroupBy();
//			System.out.println(list.size()+";;");
//			for (Integer integer : list) {
//				System.out.println(integer);
//			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			sqlSession.close();
		}
	}

}
