
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 业主
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/yezhu")
public class YezhuController {
    private static final Logger logger = LoggerFactory.getLogger(YezhuController.class);

    @Autowired
    private YezhuService yezhuService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service

    @Autowired
    private ZhiyuanzheService zhiyuanzheService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("业主".equals(role))
            params.put("yezhuId",request.getSession().getAttribute("userId"));
        else if("志愿者".equals(role))
            params.put("zhiyuanzheId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = yezhuService.queryPage(params);

        //字典表数据转换
        List<YezhuView> list =(List<YezhuView>)page.getList();
        for(YezhuView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        YezhuEntity yezhu = yezhuService.selectById(id);
        if(yezhu !=null){
            //entity转view
            YezhuView view = new YezhuView();
            BeanUtils.copyProperties( yezhu , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody YezhuEntity yezhu, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,yezhu:{}",this.getClass().getName(),yezhu.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<YezhuEntity> queryWrapper = new EntityWrapper<YezhuEntity>()
            .eq("username", yezhu.getUsername())
            .or()
            .eq("yezhu_phone", yezhu.getYezhuPhone())
            .or()
            .eq("yezhu_id_number", yezhu.getYezhuIdNumber())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YezhuEntity yezhuEntity = yezhuService.selectOne(queryWrapper);
        if(yezhuEntity==null){
            yezhu.setCreateTime(new Date());
            yezhu.setPassword("123456");
            yezhuService.insert(yezhu);
            return R.ok();
        }else {
            return R.error(511,"账户或者业主手机号或者业主身份证号已经被使用");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody YezhuEntity yezhu, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,yezhu:{}",this.getClass().getName(),yezhu.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        //根据字段查询是否有相同数据
        Wrapper<YezhuEntity> queryWrapper = new EntityWrapper<YezhuEntity>()
            .notIn("id",yezhu.getId())
            .andNew()
            .eq("username", yezhu.getUsername())
            .or()
            .eq("yezhu_phone", yezhu.getYezhuPhone())
            .or()
            .eq("yezhu_id_number", yezhu.getYezhuIdNumber())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YezhuEntity yezhuEntity = yezhuService.selectOne(queryWrapper);
        if("".equals(yezhu.getYezhuPhoto()) || "null".equals(yezhu.getYezhuPhoto())){
                yezhu.setYezhuPhoto(null);
        }
        if(yezhuEntity==null){
            yezhuService.updateById(yezhu);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"账户或者业主手机号或者业主身份证号已经被使用");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        yezhuService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<YezhuEntity> yezhuList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("../../upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            YezhuEntity yezhuEntity = new YezhuEntity();
//                            yezhuEntity.setUsername(data.get(0));                    //账户 要改的
//                            //yezhuEntity.setPassword("123456");//密码
//                            yezhuEntity.setYezhuName(data.get(0));                    //业主姓名 要改的
//                            yezhuEntity.setYezhuPhone(data.get(0));                    //业主手机号 要改的
//                            yezhuEntity.setYezhuIdNumber(data.get(0));                    //业主身份证号 要改的
//                            yezhuEntity.setYezhuPhoto("");//详情和图片
//                            yezhuEntity.setSexTypes(Integer.valueOf(data.get(0)));   //性别 要改的
//                            yezhuEntity.setYezhuEmail(data.get(0));                    //电子邮箱 要改的
//                            yezhuEntity.setCreateTime(date);//时间
                            yezhuList.add(yezhuEntity);


                            //把要查询是否重复的字段放入map中
                                //账户
                                if(seachFields.containsKey("username")){
                                    List<String> username = seachFields.get("username");
                                    username.add(data.get(0));//要改的
                                }else{
                                    List<String> username = new ArrayList<>();
                                    username.add(data.get(0));//要改的
                                    seachFields.put("username",username);
                                }
                                //业主手机号
                                if(seachFields.containsKey("yezhuPhone")){
                                    List<String> yezhuPhone = seachFields.get("yezhuPhone");
                                    yezhuPhone.add(data.get(0));//要改的
                                }else{
                                    List<String> yezhuPhone = new ArrayList<>();
                                    yezhuPhone.add(data.get(0));//要改的
                                    seachFields.put("yezhuPhone",yezhuPhone);
                                }
                                //业主身份证号
                                if(seachFields.containsKey("yezhuIdNumber")){
                                    List<String> yezhuIdNumber = seachFields.get("yezhuIdNumber");
                                    yezhuIdNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> yezhuIdNumber = new ArrayList<>();
                                    yezhuIdNumber.add(data.get(0));//要改的
                                    seachFields.put("yezhuIdNumber",yezhuIdNumber);
                                }
                        }

                        //查询是否重复
                         //账户
                        List<YezhuEntity> yezhuEntities_username = yezhuService.selectList(new EntityWrapper<YezhuEntity>().in("username", seachFields.get("username")));
                        if(yezhuEntities_username.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(YezhuEntity s:yezhuEntities_username){
                                repeatFields.add(s.getUsername());
                            }
                            return R.error(511,"数据库的该表中的 [账户] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                         //业主手机号
                        List<YezhuEntity> yezhuEntities_yezhuPhone = yezhuService.selectList(new EntityWrapper<YezhuEntity>().in("yezhu_phone", seachFields.get("yezhuPhone")));
                        if(yezhuEntities_yezhuPhone.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(YezhuEntity s:yezhuEntities_yezhuPhone){
                                repeatFields.add(s.getYezhuPhone());
                            }
                            return R.error(511,"数据库的该表中的 [业主手机号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                         //业主身份证号
                        List<YezhuEntity> yezhuEntities_yezhuIdNumber = yezhuService.selectList(new EntityWrapper<YezhuEntity>().in("yezhu_id_number", seachFields.get("yezhuIdNumber")));
                        if(yezhuEntities_yezhuIdNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(YezhuEntity s:yezhuEntities_yezhuIdNumber){
                                repeatFields.add(s.getYezhuIdNumber());
                            }
                            return R.error(511,"数据库的该表中的 [业主身份证号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        yezhuService.insertBatch(yezhuList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }


    /**
    * 登录
    */
    @IgnoreAuth
    @RequestMapping(value = "/login")
    public R login(String username, String password, String captcha, HttpServletRequest request) {
        YezhuEntity yezhu = yezhuService.selectOne(new EntityWrapper<YezhuEntity>().eq("username", username));
        if(yezhu==null || !yezhu.getPassword().equals(password))
            return R.error("账号或密码不正确");
        //  // 获取监听器中的字典表
        // ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        // Map<String, Map<Integer, String>> dictionaryMap= (Map<String, Map<Integer, String>>) servletContext.getAttribute("dictionaryMap");
        // Map<Integer, String> role_types = dictionaryMap.get("role_types");
        // role_types.get(.getRoleTypes());
        String token = tokenService.generateToken(yezhu.getId(),username, "yezhu", "业主");
        R r = R.ok();
        r.put("token", token);
        r.put("role","业主");
        r.put("username",yezhu.getYezhuName());
        r.put("tableName","yezhu");
        r.put("userId",yezhu.getId());
        return r;
    }

    /**
    * 注册
    */
    @IgnoreAuth
    @PostMapping(value = "/register")
    public R register(@RequestBody YezhuEntity yezhu){
//    	ValidatorUtils.validateEntity(user);
        Wrapper<YezhuEntity> queryWrapper = new EntityWrapper<YezhuEntity>()
            .eq("username", yezhu.getUsername())
            .or()
            .eq("yezhu_phone", yezhu.getYezhuPhone())
            .or()
            .eq("yezhu_id_number", yezhu.getYezhuIdNumber())
            ;
        YezhuEntity yezhuEntity = yezhuService.selectOne(queryWrapper);
        if(yezhuEntity != null)
            return R.error("账户或者业主手机号或者业主身份证号已经被使用");
        yezhu.setCreateTime(new Date());
        yezhuService.insert(yezhu);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @GetMapping(value = "/resetPassword")
    public R resetPassword(Integer  id){
        YezhuEntity yezhu = new YezhuEntity();
        yezhu.setPassword("123456");
        yezhu.setId(id);
        yezhuService.updateById(yezhu);
        return R.ok();
    }


    /**
     * 忘记密码
     */
    @IgnoreAuth
    @RequestMapping(value = "/resetPass")
    public R resetPass(String username, HttpServletRequest request) {
        YezhuEntity yezhu = yezhuService.selectOne(new EntityWrapper<YezhuEntity>().eq("username", username));
        if(yezhu!=null){
            yezhu.setPassword("123456");
            boolean b = yezhuService.updateById(yezhu);
            if(!b){
               return R.error();
            }
        }else{
           return R.error("账号不存在");
        }
        return R.ok();
    }


    /**
    * 获取用户的session用户信息
    */
    @RequestMapping("/session")
    public R getCurrYezhu(HttpServletRequest request){
        Integer id = (Integer)request.getSession().getAttribute("userId");
        YezhuEntity yezhu = yezhuService.selectById(id);
        if(yezhu !=null){
            //entity转view
            YezhuView view = new YezhuView();
            BeanUtils.copyProperties( yezhu , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }
    }


    /**
    * 退出
    */
    @GetMapping(value = "logout")
    public R logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return R.ok("退出成功");
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        // 没有指定排序字段就默认id倒序
        if(StringUtil.isEmpty(String.valueOf(params.get("orderBy")))){
            params.put("orderBy","id");
        }
        PageUtils page = yezhuService.queryPage(params);

        //字典表数据转换
        List<YezhuView> list =(List<YezhuView>)page.getList();
        for(YezhuView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        YezhuEntity yezhu = yezhuService.selectById(id);
            if(yezhu !=null){


                //entity转view
                YezhuView view = new YezhuView();
                BeanUtils.copyProperties( yezhu , view );//把实体数据重构到view中

                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody YezhuEntity yezhu, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,yezhu:{}",this.getClass().getName(),yezhu.toString());
        Wrapper<YezhuEntity> queryWrapper = new EntityWrapper<YezhuEntity>()
            .eq("username", yezhu.getUsername())
            .or()
            .eq("yezhu_phone", yezhu.getYezhuPhone())
            .or()
            .eq("yezhu_id_number", yezhu.getYezhuIdNumber())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YezhuEntity yezhuEntity = yezhuService.selectOne(queryWrapper);
        if(yezhuEntity==null){
            yezhu.setCreateTime(new Date());
        yezhu.setPassword("123456");
        yezhuService.insert(yezhu);
            return R.ok();
        }else {
            return R.error(511,"账户或者业主手机号或者业主身份证号已经被使用");
        }
    }


}
