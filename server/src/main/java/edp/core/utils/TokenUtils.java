/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.core.utils;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Maps;
import com.zhixue.auth.model.User;

import edp.core.consts.Consts;
import edp.core.model.TokenDetail;
import edp.davinci.core.common.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static edp.core.consts.Consts.EMPTY;


@Slf4j
@Component
public class TokenUtils {

    /**
     * 自定义 token 私钥
     */
//    @Value("${jwtToken.secret:Pa@ss@Word}")
    private String SECRET = "Pa@ss@Word";

    /**
     * 默认 token 超时时间
     */
    @Value("${jwtToken.timeout:1800000}")
    private Long TIMEOUT ;

    /**
     * 第三方平台AcessToken超时时间
     */
    @Value("${accessToken.timeout:1800000}")
    private Long ACESS_TIMEOUT;

    /**
     * 第三方平台AcessToken超时时间,60分钟
     */
    @Value("${jwtToken.timeout:3600000}")
    private Long REFRESH_TIMEOUT = 3600000L;

    /**
     * 默认 jwt 生成算法
     */
//    @Value("${jwtToken.algorithm:HS512}")
    private String ALGORITHM = "HS512";

    @PostConstruct
    public void init() {
        log.info("ACCESS_TIMEOUT = {}, TIMEOUT = {}", ACESS_TIMEOUT, TIMEOUT);
    }

    /**
     * 根据 TokenDetail 实体生成Token
     *
     * @param tokenDetail
     * @return
     */
    public String generateToken(TokenDetail tokenDetail) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Consts.TOKEN_USER_NAME, StringUtils.isEmpty(tokenDetail.getUsername()) ? EMPTY : tokenDetail.getUsername());
        claims.put(Consts.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenDetail.getPassword()) ? EMPTY : tokenDetail.getPassword());
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    /**
     * 根据com.zhixue.auth.model.User 生成token
     *
     * @param user
     * @return
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Consts.TOKEN_USER_NAME, StringUtils.isEmpty(user.getId()) ? EMPTY : user.getId());
        claims.put(Consts.TOKEN_USER_PASSWORD, EMPTY);
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    /**
     * 刷新token
     *
     * @param token
     * @return
     */
    public String refreshToken(String token) {
        Claims claims = getClaims(token);
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }


    public String refreshAcessTokenByRefreshToken(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        claims.put(Consts.TOKEN_USER_NAME, Consts.ACESS_KEY);
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims, ACESS_TIMEOUT);
    }


    /**
     * 根据 TokenDetail 实体和自定义超时时长生成Token
     *
     * @param tokenDetail
     * @param timeOutMillis （毫秒）
     * @return
     */
    public String generateToken(TokenDetail tokenDetail, Long timeOutMillis) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Consts.TOKEN_USER_NAME, StringUtils.isEmpty(tokenDetail.getUsername()) ? EMPTY : tokenDetail.getUsername());
        claims.put(Consts.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenDetail.getPassword()) ? EMPTY : tokenDetail.getPassword());
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());

        Long expiration = Long.parseLong(claims.get(Consts.TOKEN_CREATE_TIME) + EMPTY) + timeOutMillis;

        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET.getBytes("UTF-8"))
                    .compact();
        } catch (UnsupportedEncodingException ex) {
//            log.warn(ex.getMessage());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET)
                    .compact();
        }
    }

    /**
     * 根据 TokenDetail 实体生成永久 Token
     *
     * @param tokenDetail
     * @return
     */
    public String generateContinuousToken(TokenDetail tokenDetail) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Consts.TOKEN_USER_NAME, StringUtils.isEmpty(tokenDetail.getUsername()) ? EMPTY : tokenDetail.getUsername());
        claims.put(Consts.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenDetail.getPassword()) ? EMPTY : tokenDetail.getPassword());
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET.getBytes("UTF-8"))
                    .compact();
        } catch (UnsupportedEncodingException ex) {
//            log.warn(ex.getMessage());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET)
                    .compact();
        }
    }

    /**
     * 生成问卷token
     *
     * @param paperId  问卷id
     * @param info     问卷信息,json字符串
     *                 {
     *                 type, 0-匿名访问 1-署名访问 2-第三方访问
     *                 clientId, type=2是有效，为第三方id
     *                 username, type=2是有效
     *                 }
     * @param deadline 截至日期
     * @return
     */
    public String generatePaperToken(Long paperId, String info, Date deadline) {
        Map<String, Object> claims = Maps.newHashMap();
        claims.put(Consts.TOKEN_USER_NAME, paperId);
        claims.put(Consts.TOKEN_USER_PASSWORD, info);
        return generate(claims, deadline);
    }

    /**
     * 根据 clams生成token
     *
     * @param claims
     * @return
     */
    private String generate(Map<String, Object> claims) {
        Long expiration = Long.parseLong(claims.get(Consts.TOKEN_CREATE_TIME) + EMPTY) + TIMEOUT;
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET.getBytes("UTF-8"))
                    .compact();
        } catch (UnsupportedEncodingException ex) {
//            log.warn(ex.getMessage());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET)
                    .compact();
        }
    }

    /**
     * 根据 clams和timeout生成token
     *
     * @param claims
     * @return
     */
    private String generate(Map<String, Object> claims, Long timeout) {
        Long expiration = Long.parseLong(claims.get(Consts.TOKEN_CREATE_TIME) + EMPTY) + timeout;
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET.getBytes("UTF-8"))
                    .compact();
        } catch (UnsupportedEncodingException ex) {
//            log.warn(ex.getMessage());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(new Date(expiration))
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET)
                    .compact();
        }
    }

    private String generate(Map<String, Object> claims, Date deadline) {
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(deadline)
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET.getBytes("UTF-8"))
                    .compact();
        } catch (UnsupportedEncodingException ex) {
//            log.warn(ex.getMessage());
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(claims.get(Consts.TOKEN_USER_NAME).toString())
                    .setExpiration(deadline)
                    .signWith(null != SignatureAlgorithm.valueOf(ALGORITHM) ?
                            SignatureAlgorithm.valueOf(ALGORITHM) :
                            SignatureAlgorithm.HS512, SECRET)
                    .compact();
        }
    }

    public static void main(String[] args) {
        TokenUtils tokenUtils = new TokenUtils();
        String shareToken = "DA1C3E7DAF7EC46FDC39861F361C78B7517FC6CEF4F1ED425FFF75A2CB7527D3ABD4F0725AF12462EE9485E57D5234019D7F46B10755F23745011C06DE309F12B368129E480ADE8A866466422E65CB876207EF35601624A13ED676169A3788B412602E993DC452DFD3206FCE8D555B6A38250D93E66FEF9DE31678BA24C52CA786999E8145B46CBA82D6A1608622C3165A26BAAFA0F0421235E814D33428BD29EE370C2605F78ADC03099DF4280A9B26BCCEEF6839D43119D491A5C1BDACF4C76BA7028F72EE2DE1E6E4DED8301BEBA1B3EB8C5DC53F54E4FFE4EC01FBF9283907865F37B92C5F2C7F552F962A2F08C5199D5433C2AEC9F4F1E4A53C1DFC2023C7A026BC5F3D0E4739A9F70725A53AC4";
        //AES解密
        String decrypt = AESUtils.decrypt(shareToken, null);

        //获取分享信息
        String tokenUserName = tokenUtils.getUsername(decrypt);
        String tokenPassword = tokenUtils.getPassword(decrypt);
        System.out.println("");
    }

    /**
     * 解析 token 用户名
     *
     * @param token
     * @return
     */
    public String getUsername(String token) {
        String username;
        try {
            final Claims claims = getClaims(token);
            username = claims.get(Consts.TOKEN_USER_NAME).toString();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    /**
     * 解析 token 密码
     *
     * @param token
     * @return
     */
    public String getPassword(String token) {
        String password;
        try {
            final Claims claims = getClaims(token);
            password = claims.get(Consts.TOKEN_USER_PASSWORD).toString();
        } catch (Exception e) {
            password = null;
        }
        return password;
    }

    /**
     * 获取token claims
     *
     * @param token
     * @return
     */
    private Claims getClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(SECRET.getBytes("UTF-8"))
                    .parseClaimsJws(token.startsWith(Consts.TOKEN_PREFIX) ?
                            token.substring(token.indexOf(Consts.TOKEN_PREFIX) + Consts.TOKEN_PREFIX.length()).trim() :
                            token.trim())
                    .getBody();
        } catch (Exception e) {
//            log.warn(e.getMessage());
            claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.startsWith(Consts.TOKEN_PREFIX) ?
                            token.substring(token.indexOf(Consts.TOKEN_PREFIX) + Consts.TOKEN_PREFIX.length()).trim() :
                            token.trim())
                    .getBody();
        }
        return claims;
    }

    /**
     * 根据 TokenDetail 验证token
     *
     * @param token
     * @param tokenDetail
     * @return
     */
    public Boolean validateToken(String token, TokenDetail tokenDetail) {
        TokenDetail user = (TokenDetail) tokenDetail;
        String username = getUsername(token);
        String password = getPassword(token);
        return (username.equals(user.getUsername()) && password.equals(user.getPassword()) && !(isExpired(token)));
    }

    /**
     * 根据 用户名、密码 验证 token
     *
     * @param token
     * @param username
     * @param password
     * @return
     */
    public Boolean validateToken(String token, String username, String password) {
        String tokenUsername = getUsername(token);
        String tokenPassword = getPassword(token);
        return (username.equals(tokenUsername) && password.equals(tokenPassword) && !(isExpired(token)));
    }

    /**
     * 根据clientId生产第三方授权ACESS_TOKEN
     *
     * @param clientId
     * @return
     */
    public String generatePlatformAcessToken(String clientId, String extraInfo) {
        String name = StringUtils.isEmpty(extraInfo) ? Consts.ACESS_KEY : Consts.ACESS_KEY
                + Constants.SPLIT_CHAR_STRING + extraInfo;
        Map<String, Object> claims = generateClaims(name, clientId);
        return generate(claims, ACESS_TIMEOUT);
    }

    /**
     * 根据clientId生成第三方授权REFRESH_TOKEN
     *
     * @param clientId
     * @return
     */
    public String generatePlatformRefreshToken(String clientId, String extraInfo) {
        String name = StringUtils.isEmpty(extraInfo) ? Consts.ACESS_KEY : Consts.ACESS_KEY
                + Constants.SPLIT_CHAR_STRING + extraInfo;
        Map<String, Object> claims = generateClaims(Consts.REFRESH_KEY, clientId);
        return generate(claims, REFRESH_TIMEOUT);
    }

    private Map<String, Object> generateClaims(String name, String password) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Consts.TOKEN_USER_NAME, StringUtils.isEmpty(name) ? EMPTY : name);
        claims.put(Consts.TOKEN_USER_PASSWORD, StringUtils.isEmpty(password) ? EMPTY : password);
        claims.put(Consts.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return claims;
    }

    /**
     * 解析 token 创建时间
     *
     * @param token
     * @return
     */
    private Date getCreatedDate(String token) {
        Date created;
        try {
            final Claims claims = getClaims(token);
            created = new Date((Long) claims.get(Consts.TOKEN_CREATE_TIME));
        } catch (Exception e) {
            created = null;
        }
        return created;
    }

    /**
     * 获取 token 超时时间
     *
     * @param token
     * @return
     */
    private Date getExpirationDate(String token) {
        Date expiration;
        try {
            final Claims claims = getClaims(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    /**
     * token 是否超时
     *
     * @param token
     * @return
     */
    public Boolean isExpired(String token) {
        final Date expiration = getExpirationDate(token);
        //超时时间为空则永久有效
        return null == expiration ? false : expiration.before(new Date(System.currentTimeMillis()));
    }

    public Boolean isExpiredForPlatformShareToken(String token) {
        final Date expiration = getExpirationDate(token);
        return null == expiration ? true : expiration.before(new Date(System.currentTimeMillis()));
    }
}
