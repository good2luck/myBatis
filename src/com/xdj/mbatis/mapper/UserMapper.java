package com.xdj.mbatis.mapper;

import java.util.List;

import com.xdj.mbatis.model.User;

/**
 * @description 
 * @author xudj
 * @date 2016年4月21日 下午12:48:28
 * @version 1.0
 */
public interface UserMapper {
	/**
	 * @description 按id查询
	 * @author xudj
	 * @date 2016年4月21日 下午12:49:34
	 * @param id
	 * @return
	 */
	public User getUserById(Integer id);
	
	public List<Integer> testGroupBy();
	
}
