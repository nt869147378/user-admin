package com.wasu.useradmin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wasu.useradmin.entity.ResultData;
import com.wasu.useradmin.entity.User;
import com.wasu.useradmin.entity.UserDto;
import com.wasu.useradmin.mapper.UserMapper;
import com.wasu.useradmin.service.UserService;
import org.apache.logging.log4j.message.ReusableMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;
import org.yaml.snakeyaml.events.Event;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;


    @GetMapping("/")
    public String index(){
        return "login";
    }

    @RequestMapping(value = "/userList/{username}", method = RequestMethod.GET)
    public String userList(@PathVariable("username") String username,
                           Model model,
                           HttpServletRequest request){
        model.addAttribute("username",username);
        model.addAttribute("currentUser",userService.searchUserByName(username));
        model.addAttribute("userList",userService.searchAllUsers());
        return "userList";
    }

    @PostMapping("/userInfo")
    @ResponseBody
    public String getUserInfo(@RequestParam("userId") String userId,
                           Model model,
                           HttpServletRequest request){
        User user = userService.searchById(Integer.parseInt(userId));
        if(user == null) return JSON.toJSONString(ResultData.fail("获取不到用户信息"));
        model.addAttribute("userInfo",user);
        List<Object> res = new ArrayList<>();
        res.add(user);
        //System.out.println(user);
        return JSON.toJSONString(ResultData.success(res));
    }

    @GetMapping("/userInfoList")
    @ResponseBody
    public String getUserInfoList(@RequestParam("userIdList") String userIdList){
        List<String> idList = JSON.parseObject(userIdList,ArrayList.class);
        List<User> users = new ArrayList<>();
        Collections.sort(idList);
        for(String id : idList){
            users.add(userService.searchById(Integer.parseInt(id)));
        }
        List<Object> res = new ArrayList<>();
        res.add(users);
        return JSON.toJSONString(ResultData.success(res));
    }

    @GetMapping("/error")
    public String error(){
        return "/error";
    }


    @PostMapping("/register")
    @ResponseBody
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam("sex") int sex,
                           @RequestParam("phone") String phone,
                           @RequestParam("address") String address,
                           @RequestParam("verifyCode") String verifyCode,
                           Model model,
                           HttpServletResponse response,
                           HttpServletRequest request){
        //非空验证
        if(StringUtils.isEmptyOrWhitespace(username) || StringUtils.isEmptyOrWhitespace(password)) {
            return JSON.toJSONString(ResultData.fail("用户名或密码不能为空，请重试"));
        }

        //验证手机号长度
        if(phone.length() != 11){
            return JSON.toJSONString(ResultData.fail("手机号长度只能为11，请重试"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setSex(sex);
        user.setPhone(phone);
        user.setAddress(address);

        //检验验证码输入
        String kaptchaCode = (String) request.getSession().getAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        if(!StringUtils.equalsIgnoreCase(verifyCode,kaptchaCode)){
            return JSON.toJSONString(ResultData.fail("验证码输入错误，请重试"));
        }

        //验证用户名
        int n = userService.searchByName(username);
        if(n > 0){
            return JSON.toJSONString(ResultData.fail("该用户已存在，请重试"));
        }

        //插入数据库
        int res = userService.insert(user);
        //获取自增ID
        int id = userService.searchId();
        user.setId(id);

        if(res > 0){
            //插入Redis
            ValueOperations opsForValue = redisTemplate.opsForValue();
            opsForValue.set(username,user);//数据结构：key:username value:user对象
            opsForValue.getAndExpire(username, 7,TimeUnit.DAYS);//7天过期
            return JSON.toJSONString(ResultData.success("注册成功",userService.searchAllUsers()));
        }else{
            return JSON.toJSONString(ResultData.fail("系统异常，请重试"));
        }

    }

    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestParam("username") String username,
                         @RequestParam("password") String password,
                         @RequestParam("verifyCode") String verifyCode,
                         Model model,
                         HttpServletResponse response,
                         HttpServletRequest request){
        //非空验证
        if(StringUtils.isEmptyOrWhitespace(username) || StringUtils.isEmptyOrWhitespace(password)) {
            return JSON.toJSONString(ResultData.fail("用户名或密码不能为空，请重试"));
        }

        //检验验证码输入
        String kaptchaCode = (String) request.getSession().getAttribute(com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY);
        if(!StringUtils.equalsIgnoreCase(verifyCode,kaptchaCode)){
            return JSON.toJSONString(ResultData.fail("验证码输入错误，请重试"));
        }

        //从Redis查
        ValueOperations opsForValue = redisTemplate.opsForValue();

        User user = (User) opsForValue.get(username);

        if(user == null || !user.getPassword().equals(password)){
            //如果从Redis找不到再从数据库找
            user = userService.searchUserByName(username);
            //还是找不到，返回结果
            if(user == null || !user.getPassword().equals(password)){
                return JSON.toJSONString(ResultData.fail("用户名或密码错误，请重试"));
            }else{
                //在数据库中找到，并更新到redis
                opsForValue.set(username,user);
                opsForValue.getAndExpire(username,7, TimeUnit.DAYS);//设置失效日期
                List<Object> resList = new ArrayList<>();
                resList.add(username);
                resList.add(userService.searchAllUsers());
                return JSON.toJSONString(ResultData.success("登录成功",resList));
            }
        }else{
            opsForValue.getAndExpire(username,7, TimeUnit.DAYS);//更新失效日期
            List<Object> resList = new ArrayList<>();
            resList.add(username);
            resList.add(userService.searchAllUsers());
            return JSON.toJSONString(ResultData.success("登录成功",resList));
        }
    }

    @RequestMapping(value = "/change/{username}", method = RequestMethod.GET)
    public String change(@PathVariable("username") String username,
                         HttpServletRequest request,
                         Model model){
        ValueOperations opsForValue = redisTemplate.opsForValue();
        User user = (User) opsForValue.get(username);
        if(user == null){
            return "error";
        }
        model.addAttribute("username",username);
        return "change";
    }

    @PostMapping("/changeword")
    @ResponseBody
    public String changepwd(@RequestParam("username") String username,
                            @RequestParam("prePassword") String prePassword,
                            @RequestParam("password") String password,
                            HttpServletRequest request){
        //从redis中根据username找user
        ValueOperations opsForValue = redisTemplate.opsForValue();
        User user = (User) opsForValue.get(username);
        if(user == null){
            return JSON.toJSONString(ResultData.fail("获取用户失败，请重试"));
        }

        if(!prePassword.equals(user.getPassword())){
            return JSON.toJSONString(ResultData.fail("原密码错误，请重试"));
        }
        if(prePassword.equals(password)){
            return JSON.toJSONString(ResultData.fail("修改后的密码不能与原密码相同，请重试"));
        }
        if(StringUtils.isEmptyOrWhitespace(password)){
            return JSON.toJSONString(ResultData.fail("密码不能为空，请重试"));
        }

        //修改数据库
        user.setPassword(password);
        int res = userService.updateById(user);
        //更新redis
        opsForValue.set(username,user);

        //修改成功
        if(res > 0){
            return JSON.toJSONString(ResultData.success("修改成功，请重新登录"));
        }else{
            return JSON.toJSONString(ResultData.error());
        }
    }

    @PostMapping("/update")
    @ResponseBody
    public String changeUserInfo(@RequestParam("id") int id,
                                     @RequestParam("sex") int sex,
                                     @RequestParam("phone") String phone,
                                     @RequestParam("address") String address,
                                     HttpServletRequest request){
        User user = userService.searchById(id);
        user.setSex(sex);
        user.setPhone(phone);
        user.setAddress(address);
        //验证手机号长度
        if(phone.length() != 11){
            return JSON.toJSONString(ResultData.fail("手机号长度只能为11，请重试"));
        }
        int res = userService.updateById(user);
        if(res > 0){
            //插入Redis
            ValueOperations opsForValue = redisTemplate.opsForValue();
            opsForValue.set(user.getUsername(),user);//数据结构：key:username value:user对象
            opsForValue.getAndExpire(user.getUsername(), 7,TimeUnit.DAYS);//7天过期
            return JSON.toJSONString(ResultData.success("修改成功",userService.searchAllUsers()));
        }else{
            return JSON.toJSONString(ResultData.fail("系统异常，修改失败"));
        }
    }

    @PostMapping("/multiUpdate")
    @ResponseBody
    public String multiUpdate(@RequestParam("userList") String userList){
        List<String> list = JSON.parseObject(userList, ArrayList.class);
        int count = 0;
        int total = list.size();
        for (int i = 0; i < list.size(); i++) {
            UserDto userDto = JSON.parseObject(list.get(i), UserDto.class);
            if(userDto.getPhone().length() != 11) return JSON.toJSONString(ResultData.fail("手机号长度只能为11，请重试"));
            User user = userService.searchById(userDto.getId());
            user.setSex(userDto.getSex());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            //插入数据库
            count += userService.updateById(user);
            //插入redis
            ValueOperations opsForValue = redisTemplate.opsForValue();
            opsForValue.set(user.getUsername(),user);
        }
        if(count == total){
            return JSON.toJSONString(ResultData.success("修改成功"));
        }else{
            return JSON.toJSONString(ResultData.fail("修改失败"));
        }
    }

    @GetMapping("/search")
    @ResponseBody
    public String search(@RequestParam("condition") String condition){
        HashMap map = JSON.parseObject(condition, HashMap.class);
        String id = (String) map.get("id");
        String username = (String) map.get("username");
        String sex = (String) map.get("sex");
        String phone = (String) map.get("phone");
        String address = (String) map.get("address");

        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if(!StringUtils.isEmptyOrWhitespace(id)) wrapper.eq("id", id);
        if(!StringUtils.isEmptyOrWhitespace(username)) wrapper.like("username", username);
        if(!StringUtils.isEmptyOrWhitespace(sex) && !sex.equals("2")) wrapper.eq("sex", sex);
        if(!StringUtils.isEmptyOrWhitespace(phone)) wrapper.like("phone", phone);
        if(!StringUtils.isEmptyOrWhitespace(address)) wrapper.like("address", address);

        List<User> users = userService.search(wrapper);
        List<Object> res = new ArrayList<>();
        res.add(users);
        return JSON.toJSONString(ResultData.success(res));
    }

//    @RequestMapping("/test")
//    @ResponseBody
//    public String test(){
//        QueryWrapper<User> wrapper = new QueryWrapper<>();
//        wrapper.like("username","");
//        List<User> list = userMapper.selectList(wrapper);
//        System.out.println(list.size());
//        System.out.println(list.get(0));
//        return "123";
//    }
}
