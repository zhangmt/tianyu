package com.ty.tianyu.system.service;

import java.sql.Timestamp;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ty.tianyu.common.persistence.HibernateDao;
import com.ty.tianyu.common.service.BaseService;
import com.ty.tianyu.common.utils.DateUtils;
import com.ty.tianyu.common.utils.security.Digests;
import com.ty.tianyu.common.utils.security.Encodes;
import com.ty.tianyu.system.dao.UserDao;
import com.ty.tianyu.system.entity.User;
import com.ty.tianyu.system.utils.UserUtil;

/**
 * 用户service
 * @author ty
 * @date 2014年8月19日 下午3:28:44
 */
@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User, Integer> {
	
	/**加密方法*/
	public static final String HASH_ALGORITHM = "SHA-1";
	public static final int HASH_INTERATIONS = 1024;
	private static final int SALT_SIZE = 8;	//盐长度

	@Autowired
	private UserDao userDao;

	@Override
	public HibernateDao<User, Integer> getEntityDao() {
		return userDao;
	}
	

	/**
	 * 保存用户
	 * @param user
	 */
	@Transactional(readOnly=false)
	public void saveUser(User user) {
		entryptPassword(user);
		user.setCreater(UserUtil.getCurrentUser().getName());
		user.setCreateDate(new Timestamp(new Date().getTime()));
		userDao.save(user);
	}

	

	/**
	 * 修改用户
	 * @param user
	 */
	@Transactional(readOnly=false)
	public void updateUser(User user) {
		user.setModifier(UserUtil.getCurrentUser().getName());
		user.setModifyDate(new Timestamp(new Date().getTime()));
		userDao.save(user);
	}
	
	/**
	 * 修改密码
	 * @param user
	 */
	@Transactional(readOnly=false)
	public void updatePwd(User user) {
		entryptPassword(user);
		userDao.save(user);
	}
	
	/**
	 * 删除用户
	 * @param id
	 */
	@Transactional(readOnly=false)
	public void deleteUser(Integer id){
		if(!isSupervisor(id))
			userDao.delete(id);
	}
	
	
	/**
	 * 按登录名查询用户
	 * @param loginName
	 * @return 用户对象
	 */
	public User getUser(String loginName) {
		return userDao.findUniqueBy("loginName", loginName);
	}
	
	/**
	 * 判断是否超级管理员
	 * @param id
	 * @return boolean
	 */
	private boolean isSupervisor(Integer id) {
		return id == 1;
	}
	
	/**
	 * 设定安全的密码，生成随机的salt并经过1024次 sha-1 hash
	 */
	private void entryptPassword(User user) {
		byte[] salt = Digests.generateSalt(SALT_SIZE);
		user.setSalt(Encodes.encodeHex(salt));

		byte[] hashPassword = Digests.sha1(user.getPlainPassword().getBytes(),salt, HASH_INTERATIONS);
		user.setPassword(Encodes.encodeHex(hashPassword));
	}
	
	/**
	 * 验证原密码是否正确
	 * @param user
	 * @param oldPwd
	 * @return
	 */
	public boolean checkPassword(User user,String oldPassword){
		byte[] salt =Encodes.decodeHex(user.getSalt()) ;
		byte[] hashPassword = Digests.sha1(oldPassword.getBytes(),salt, HASH_INTERATIONS);
		if(user.getPassword().equals(Encodes.encodeHex(hashPassword))){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 修改用户登录
	 * @param user
	 */
	public void updateUserLogin(User user){
		user.setLoginCount((user.getLoginCount()==null?0:user.getLoginCount())+1);
		user.setPreviousVisit(user.getLastVisit());
		user.setLastVisit(DateUtils.getSysTimestamp());
		updateUser(user);
	}

	
}