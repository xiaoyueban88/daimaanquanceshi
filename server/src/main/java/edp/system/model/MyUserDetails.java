/**
 * 实现spring security的UserDetails
 */
package edp.system.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import com.zhixue.auth.model.Authority;
import com.zhixue.auth.model.User;

/**
 * 
 * @author zhangkaixuan
 *
 */
public class MyUserDetails implements UserDetails {
	
	private static final long serialVersionUID = 2320489511021456169L;

	private List<Authority> privileges;
	
	/**
	 * 当前登录用户
	 */
	private User currentMember;
	
	public MyUserDetails(User currentMember, List<Authority> privileges){
		this.privileges = privileges;
		this.currentMember = currentMember;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// 返回用户所具备的权限列表
		if (privileges == null || privileges.size() < 1) {
			return AuthorityUtils.commaSeparatedStringToAuthorityList("");
		}
		StringBuilder commaBuilder = new StringBuilder();
		for(Authority privilege : privileges){
			commaBuilder.append(privilege.getId()).append(",");
		}
		String authorities = commaBuilder.substring(0,commaBuilder.length()-1);
		return AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return currentMember.getId();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	/**
	 * @return the currentMember
	 */
	public User getCurrentMember() {
		return currentMember;
	}

	/**
	 * @param currentMember the currentMember to set
	 */
	public void setCurrentMember(User currentMember) {
		this.currentMember = currentMember;
	}
	
	

}
