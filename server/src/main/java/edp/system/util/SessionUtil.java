/**
 * 
 */
package edp.system.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.zhixue.auth.model.User;

import edp.system.model.MyUserDetails;

/**
 * 获取当前登录用户的信息
 * 
 * @author zhangkaixuan
 *
 */
@Component
public class SessionUtil {

	/**
	 * 获取当前登录用户信息
	 * 
	 * @return
	 */
	public User getCurrentUser() {
		try {
			MyUserDetails userDetails = (MyUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			return userDetails.getCurrentMember();
		} catch (Exception e) {
			return null;
		}
	}

}
