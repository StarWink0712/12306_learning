package toolkit;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.sql.rowset.spi.SyncResolver;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.guoxu.constant.UserConstant.*;

/**
 * JWTUtil jwt工具类，用于生成和解析token令牌
 *
 * @author 执笔画棠
 * @date 2025/11/08 20:30
 **/
@Slf4j
public final class JWTUtil {

    //token过期时86400秒 = 24小时，即Token有效期为1天
    private static final long EXPIRATION=86400L;
    //token前缀
    private static final String TOKEN_PREFIX="Bearer ";
    //token签发者标识
    private static final String ISS="index12306";
    //token密钥
    private static final String SECRET="SecretKey039245678901232039487623456783092349288901402967890140939827";


    //生成token
    public static String generateAccessToken(UserInfoDTO userInfo){
        //创建一个map，存储自定义的用户信息，
        Map<String,Object> customerUserMap=new HashMap<>();
        //向map中存入用户id
        customerUserMap.put(USER_ID_KEY,userInfo.getUserId());
        //向map中存入用户名
        customerUserMap.put(USER_NAME_KEY,userInfo.getUsername());
        //向map中存入真实姓名
        customerUserMap.put(REAL_NAME_KEY,userInfo.getRealName());

        //使用jwt构建器，构建jwt token
        String jwtToken= Jwts.builder()
                // 设置签名算法为HS512，指定签名密钥
                .signWith(SignatureAlgorithm.HS512, SECRET)
                // 设置Token签发时间为当前系统时间
                .setIssuedAt(new Date())
                // 设置Token签发者为ISS常量指定的值
                .setIssuer(ISS)
                // 设置Token主题（Subject），将用户信息Map序列化为JSON字符串存入
                .setSubject(JSON.toJSONString(customerUserMap))
                // 设置Token过期时间：当前时间 + 过期秒数（转换为毫秒）
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                // 压缩生成最终的JWT Token字符串
                .compact();


        //拼装token前缀
        return TOKEN_PREFIX+jwtToken;
    }

    //解析token
    /**
     * 解析用户访问Token
     * 验证Token有效性并提取用户信息
     *
     * @param jwtToken 前端传入的带前缀的用户访问Token
     * @return 解析成功且有效的用户信息DTO；失败或无效则返回null
     */
    public static UserInfoDTO parseJwtToken(String jwtToken) {
        // 校验传入的Token字符串是否不为空且包含有效文本
        if (StringUtils.hasText(jwtToken)) {
            // 去除Token中的前缀，获取真实的JWT Token字符串
            String actualJwtToken = jwtToken.replace(TOKEN_PREFIX, "");
            try {
                // 解析Token：设置签名密钥 -> 解析带签名的JWT -> 获取负载（Claims）
                Claims claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(actualJwtToken).getBody();
                // 从负载中获取Token过期时间
                Date expiration = claims.getExpiration();
                // 校验Token是否未过期（当前时间在过期时间之前）
                if (expiration.after(new Date())) {
                    // 从负载中获取主题（用户信息JSON字符串）
                    String subject = claims.getSubject();
                    // 将JSON字符串反序列化为UserInfoDTO对象并返回
                    return JSON.parseObject(subject, UserInfoDTO.class);
                }
            } catch (ExpiredJwtException ignored) {
                // 捕获Token过期异常，忽略处理（已通过expiration.after()校验）
            } catch (Exception ex) {
                // 捕获其他解析异常（签名不匹配、格式错误等），记录错误日志
                log.error("JWT Token解析失败，请检查", ex);
            }
        }
        // Token为空、无效或解析失败时返回null
        return null;
    }
}
