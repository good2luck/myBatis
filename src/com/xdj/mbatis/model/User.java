package com.xdj.mbatis.model;

/**
 * @description 
 * @author xudj
 * @date 2016年4月21日 下午12:47:23
 * @version 1.0
 */
public class User {
	private Integer id;			//主键
	private String na_me;		//名字
	private Integer age;		//年龄
	private Integer ag_e2;		//测试2
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getNa_me() {
		return na_me;
	}
	public void setNa_me(String na_me) {
		this.na_me = na_me;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	
	public Integer getAg_e2() {
		return ag_e2;
	}
	public void setAg_e2(Integer ag_e2) {
		this.ag_e2 = ag_e2;
	}
	@Override
	public String toString() {
		return "User [id=" + id + ", na_me=" + na_me + ", age=" + age
				+ ", ag_e2=" + ag_e2 + "]";
	}

}
