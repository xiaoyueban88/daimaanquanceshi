/**
 * 重写spring security的UserDetailsService的加载用户详情逻辑
 */
package edp.system.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zhixue.auth.model.Authority;
import com.zhixue.auth.model.Team;
import com.zhixue.auth.model.UserStatus;
import com.zhixue.auth.service.UserService;

import edp.system.model.MyUserDetails;

/**
 * @author zhangkaixuan
 *
 */
@Service
public class MyUserDetailService implements UserDetailsService {

	@Autowired
	private UserService authUserService;

	@Autowired
	Environment env;

	private Logger logger = LoggerFactory.getLogger(MyUserDetailService.class);

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
	 */
	@Override
	public UserDetails loadUserByUsername(String accountId) throws UsernameNotFoundException {
		logger.info("加载用户权限列表...");
		com.zhixue.auth.model.User user = authUserService.auth(accountId);

		List<Authority> pris = new ArrayList<>();

		if (user == null) {
			user = new com.zhixue.auth.model.User();
			user.setId(accountId);
			user.setUserName("未知");
			Team team = new Team();
			team.setId("-100");
			user.setTeam(team);
			List<Team> teams = new ArrayList<Team>();
			teams.add(team);
			user.setTeams(teams);
			user.setUserStatus(UserStatus.notActive);
			authUserService.add(user);
		}
		Authority privilege = new Authority();
		privilege.setId("BASE");
		pris.add(privilege);
		// 未激活,不查数据库，直接给一个BASE的权限
		if ("notActive".equals(user.getUserStatus())) {
			pris.add(privilege);
			MyUserDetails myUserDetails = new MyUserDetails(user, pris);
			return myUserDetails;
		}
		// 正常的已激活用户
		// 加载用户信息，这里是加载用户所具备的所有权限列表
		String seceret = env.getProperty("app.secret");
		pris = authUserService.findPrivilegesByUserId(accountId, seceret);
		if (pris == null || pris.size() == 0) {
			pris = new ArrayList<>();
		}
		pris.add(privilege);
		MyUserDetails myUserDetails = new MyUserDetails(user, pris);
		return myUserDetails;
	}
}
