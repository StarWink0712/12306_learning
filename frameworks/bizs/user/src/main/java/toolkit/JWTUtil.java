package toolkit;

import com.alibaba.fastjson2.JSON;
import core.UserInfoDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.print.DocFlavor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWTUtil工具类
 * 用于生成和解析JWT令牌
 *
 * @author 执笔画棠
 * @date 2025/11/04 21:12
 **/
@Slf4j
// 定义JWT（JSON Web Token）工具类，用于实现JWT令牌的生成与解析
public class JWTUtil {

    // 私有静态常量：JWT令牌的过期时间，单位为秒（86400秒 = 24小时）
    // 表示生成的令牌从签发时间起，24小时内有效
    private static final long EXPIRATION=86400L;

    // 私有静态常量：JWT令牌的前缀，遵循JWT标准格式（通常为"Bearer"）
    // 前端传递令牌时需携带该前缀，后端解析时需先移除前缀
    private static final String TOKEN_PREFIX="Bearer";

    // 私有静态常量：JWT的签发者（Issuer）标识，此处为"index12306"（对应12306系统）
    // 用于标识该令牌是由哪个系统/服务签发的，便于后续校验
    private static final String ISS="index12306";

    // 私有静态常量：JWT的签名密钥（Secret Key）
    // 用于生成令牌时签名、解析令牌时验证签名，需保密且足够复杂，防止被破解
    private static final String SECRET="SecretKey039245678901232039487623456783092349288901402967890140939827";


    // 静态方法：生成用户访问令牌（AccessToken）
    // 参数userInfoDTO：存储用户核心信息的DTO对象（如用户ID、用户名等）
    // 返回值：带前缀的完整JWT令牌字符串
    public static String generateAccessToken(UserInfoDTO userInfoDTO){
        // 创建HashMap，用于存储用户的自定义信息（将存入JWT的"载荷（Claims）"中）
        Map<String,Object> customerUserMap=new HashMap<>();

        // 向Map中存入用户ID，键为"USER_ID_KEY"（自定义键名，解析时需对应）
        customerUserMap.put("USER_ID_KEY",userInfoDTO.getUserId());

        // 向Map中存入用户名，键为"USER_NAME_KEY"（自定义键名，解析时需对应）
        customerUserMap.put("USER_NAME_KEY",userInfoDTO.getUsername());

        // 向Map中存入用户真实姓名，键为"REAL_USER_NAME"（自定义键名，解析时需对应）
        // 注：原代码此处语法有误（多了一个左括号），注释按逻辑功能描述
        customerUserMap.put("REAL_USER_NAME",userInfoDTO.getRealName());

        // 构建JWT令牌：通过Jwts.builder()开启构建流程
        String jwtToken= Jwts.builder()
                // 将存储用户信息的Map设置为JWT的载荷（Claims）
                .setClaims(customerUserMap)
                // 设置JWT的签发者（Issuer），值为前面定义的ISS常量
                .setIssuer(ISS)
                // 设置JWT的签发时间（Issued At），值为当前系统时间
                .setIssuedAt(new Date())
                // 设置JWT的过期时间（Expiration）
                // 计算方式：当前系统时间 + EXPIRATION秒（转成毫秒，因Date单位为毫秒）
                .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION*1000))
                // 设置JWT的签名算法和签名密钥
                // 算法为HS512（HMAC-SHA512，对称加密算法），密钥为前面定义的SECRET常量
                .signWith(SignatureAlgorithm.HS512,SECRET)
                // 完成JWT构建，生成紧凑的令牌字符串（无空格、换行的Base64编码格式）
                .compact();

        // 返回带前缀的完整令牌：将TOKEN_PREFIX（"Bearer"）与生成的令牌拼接
        // 前端后续请求需携带该完整字符串（如在Authorization请求头中）
        return TOKEN_PREFIX+jwtToken;
    }

    /**
     * 解析用户 Token
     *
     * @param jwtToken 用户访问 Token（带"Bearer"前缀的完整令牌）
     * @return 用户信息DTO对象（解析成功且令牌有效时返回，否则返回null）
     */
    public static UserInfoDTO parseJwtToken(String jwtToken) {
        // 第一步：判断传入的令牌字符串是否不为空且包含有效文本（避免空指针或空字符串）
        if (StringUtils.hasText(jwtToken)) {
            // 第二步：移除令牌的"Bearer"前缀，提取实际的JWT令牌字符串
            // 因前端传递的是带前缀的令牌，后端解析时需先去掉前缀
            String actualJwtToken = jwtToken.replace(TOKEN_PREFIX, "");

            try {
                // 第三步：解析JWT令牌
                // 1. 创建JWT解析器，设置签名密钥（必须与生成令牌时的密钥一致，否则解析失败）
                // 2. 调用parseClaimsJws()解析令牌，获取包含载荷的Jws对象（JWS：JSON Web Signature，带签名的JWT）
                // 3. 调用getBody()获取JWT的载荷（Claims），即生成令牌时存入的用户信息等数据
                Claims claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(actualJwtToken).getBody();

                // 第四步：获取令牌的过期时间，判断令牌是否未过期
                Date expiration = claims.getExpiration();
                if (expiration.after(new Date())) {
                    // 第五步：获取JWT的主题（Subject）
                    // 注：原代码存在逻辑问题——生成令牌时未调用setSubject()设置主题，此处getSubject()可能返回null
                    // 注释按代码实际行为描述，不修改逻辑
                    String subject = claims.getSubject();

                    // 将主题字符串（假设是JSON格式）解析为UserInfoDTO对象并返回
                    return JSON.parseObject(subject, UserInfoDTO.class);
                }
                // 若令牌已过期（expiration在当前时间之前），不进入上面的if，直接走后续逻辑返回null
            } catch (ExpiredJwtException ignored) {
                // 捕获"令牌已过期"异常，此处选择忽略（无需额外处理，后续自然返回null）
            } catch (Exception ex) {
                // 捕获其他解析异常（如签名错误、令牌格式错误、数据解析失败等）
                // 打印错误日志，便于问题排查（如非法令牌、密钥不匹配等）
                log.error("JWT Token解析失败，请检查", ex);
            }
        }
        // 若令牌为空、解析失败、令牌过期，均返回null
        return null;
    }
}
