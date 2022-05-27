package com.c3stones.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 系统用户信息
 * 
 * @author CL
 *
 */
@Data
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	private Integer id;

	/**
	 * 用户名称
	 */
	private String username;

	/**
	 * 用户昵称
	 */
	private String nickname;

	/**
	 * 用户密码
	 */
	private String password;

}
