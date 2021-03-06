/**
 * Cash-Register
 * Copyright (c) 1995-2018 All Rights Reserved.
 */
package cn.cash.register.controller.backstage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.PageInfo;

import cn.cash.register.common.Constants;
import cn.cash.register.common.request.MemberInfoQueryRequest;
import cn.cash.register.common.request.MemberRankQueryRequest;
import cn.cash.register.common.request.MemberRechargeCheckQueryRequest;
import cn.cash.register.common.request.MemberRechargeQueryRequest;
import cn.cash.register.dao.domain.MemberInfo;
import cn.cash.register.dao.domain.MemberIntegral;
import cn.cash.register.dao.domain.MemberRank;
import cn.cash.register.dao.domain.MemberRechargeDetail;
import cn.cash.register.dao.domain.RechargeCheckDetail;
import cn.cash.register.dao.domain.SellerInfo;
import cn.cash.register.enums.LogSourceEnum;
import cn.cash.register.enums.SubSystemTypeEnum;
import cn.cash.register.service.LogService;
import cn.cash.register.service.MemberRechargeService;
import cn.cash.register.service.MemberService;
import cn.cash.register.util.AssertUtil;
import cn.cash.register.util.DateUtil;
import cn.cash.register.util.ExcelUtil;
import cn.cash.register.util.LogUtil;
import cn.cash.register.util.ResultSet;

/**
 * 会员功能相关Controller
 * @author HuHui
 * @version $Id: MemberController.java, v 0.1 2018年4月24日 下午10:15:58 HuHui Exp $
 */
@Controller
@RequestMapping("/admin/member")
public class MemberController {

    private static final Logger   logger = LoggerFactory.getLogger(MemberController.class);

    @Resource
    private MemberService         memberService;

    @Resource
    private MemberRechargeService memberRechargeService;

    @Resource
    private LogService            logService;

    /****************************会员信息相关接口****************************/

    @GetMapping
    public String list() {
        return "backstage/_member-list";
    }

    /**
     * 会员列表
     */
    @ResponseBody
    @RequestMapping(value = "/list")
    public ResultSet queryList(MemberInfoQueryRequest request) {
        LogUtil.info(logger, "[Controller]收到#会员列表查询#请求,request={0}", request);

        PageInfo<MemberInfo> pageInfo = memberService.queryList(request);

        LogUtil.info(logger, "[Controller]#会员列表查询#请求处理,pageInfo={0}", pageInfo);
        return ResultSet.success().put("page", pageInfo);
    }

    /**
     * 会员资料导出
     */
    @SuppressWarnings("resource")
    @RequestMapping(value = "/exportList")
    public void exportList(MemberInfoQueryRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        LogUtil.info(logger, "[Controller]收到#导出会员资料列表#请求,request={0}", request);

        PageInfo<MemberInfo> pageInfo = memberService.queryList(request);

        // 根据查询结果在服务端生成excel文件
        String filePath = session.getServletContext().getRealPath(Constants.EXPORT_FILE_RELATIVE_PATH) + File.separator;
        String fileName = "会员资料导出_" + DateUtil.format(new Date(), DateUtil.msecFormat) + ".xlsx";
        String sheetName = "会员资料";

        List<List<String>> data = new ArrayList<List<String>>();
        String[] filterRow = { "等级", request.getMemberRank(), "", "导购员", request.getShopperName(), "", "状态(true:启用;false:禁用)", request.isStatus() + "", "", "卡号/姓名/电话", request.getKeyword() };
        data.add(Arrays.asList(filterRow));
        String[] pageRow = { "当前页", pageInfo.getPageNum() + "/" + pageInfo.getPages(), "每页数量", pageInfo.getPageSize() + "", "总数", pageInfo.getTotal() + "" };
        data.add(Arrays.asList(pageRow));
        String[] theadRow = { "编号", "姓名", "等级", "折扣", "积分", "电话", "密码", "生日", "允许赊账(true:允许;false:不允许)", "QQ", "邮箱", "地址", "导购员", "备注", "状态(true:启用;false:禁用)" };
        data.add(Arrays.asList(theadRow));

        List<MemberInfo> list = pageInfo.getList();
        for (MemberInfo _obj : list) {
            List<String> _row = new ArrayList<String>();
            _row.add(_obj.getMemberNo());
            _row.add(_obj.getMemberName());
            _row.add(_obj.getMemberRank());
            _row.add(ExcelUtil.obj2String(_obj.getMemberDiscount()));
            _row.add(ExcelUtil.obj2String(_obj.getMemberIntegral()));
            _row.add(_obj.getPhone());
            _row.add(_obj.getPassword());
            _row.add(_obj.getBirthday());
            _row.add(ExcelUtil.obj2String(_obj.getIsOnCredit()));
            _row.add(_obj.getQqNo());
            _row.add(_obj.getEmail());
            _row.add(_obj.getAddress());
            _row.add(_obj.getShopperName());
            _row.add(_obj.getRemark());
            _row.add(ExcelUtil.obj2String(_obj.getStatus()));
            data.add(_row);
        }
        try {
            ExcelUtil.createExcel(filePath, fileName, sheetName, data); // 文件成功生成在服务端
        } catch (IOException e) {
            LogUtil.error(e, logger, "文件生成失败");
        }

        // 返回生成文件
        InputStream bis = new BufferedInputStream(new FileInputStream(new File(filePath, fileName))); // 获取输入流
        fileName = URLEncoder.encode(fileName, "UTF-8"); // 转码，免得文件名中文乱码
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName); // 设置文件下载头
        response.setContentType("multipart/form-data"); // 设置文件ContentType类型
        BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
        int len = 0;
        while ((len = bis.read()) != -1) {
            out.write(len);
            out.flush();
        }
        out.close();
    }

    /**
     * 会员资料导入
     *
     * @param file
     * @return
     * @throws IOException 
     */
    @ResponseBody
    @PostMapping(value = "/importList")
    public ResultSet importList(MultipartFile file, HttpSession session) {
        LogUtil.info(logger, "收到会员资料导入请求");
        AssertUtil.assertNotNull(file, "系统异常:上传文件对象为空");

        SellerInfo seller = (SellerInfo) session.getAttribute(Constants.LOGIN_FLAG_ADMIN);

        // 1.接收文件
        String path = session.getServletContext().getRealPath(Constants.IMPORT_FILE_RELATIVE_PATH);
        String fileName = file.getOriginalFilename();
        LogUtil.info(logger, "文件上传请求:fileName={0}", fileName);

        File destinationFile = new File(path, fileName);
        if (!destinationFile.exists()) {
            destinationFile.mkdirs();
        }

        try {
            //MultipartFile自带的解析方法
            file.transferTo(destinationFile);
            LogUtil.info(logger, "文件上传成功,保存路径:path={0}", path);
        } catch (IllegalStateException | IOException e) {
            LogUtil.error(e, logger, "文件上传异常");
            return ResultSet.error("文件上传异常");
        }

        // 2.读取数据
        List<MemberInfo> members = null;
        try {
            List<List<String>> excelData = ExcelUtil.readExcel(destinationFile, 15); // 会员信息共有15列
            AssertUtil.assertTrue(CollectionUtils.isNotEmpty(excelData), "未读取到任何信息");
            members = memberService.transfer2MemberInfo(excelData);
        } catch (IOException e) {
            LogUtil.error(e, logger, "文件读取异常");
            return ResultSet.error("文件读取异常");
        }

        // 3.存储数据
        int successCount = 0;
        int failCount = 0;
        String failInfo = "";
        for (MemberInfo _member : members) {
            LogUtil.info(logger, "[Controller]#导入会员#,memberInfo={0}", _member);
            try {
                memberService.addMember(_member);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failInfo = failInfo + "【" + _member.toString() + "】";
            }
            logService.record(LogSourceEnum.backstage, SubSystemTypeEnum.employee, seller.getSellerNo(), "导入会员" + _member.getMemberNo());
        }

        String resultInfo = "成功导入" + successCount + "条会员信息";
        if (failCount > 0) {
            resultInfo += (",以下" + failCount + "条导入失败:" + failInfo);
        }

        return ResultSet.success(resultInfo);
    }

    /**
     * 添加或更新会员
     */
    @ResponseBody
    @RequestMapping(value = "/addOrUpdate")
    public ResultSet addOrUpdate(MemberInfo memberInfo, HttpSession session) {
        LogUtil.info(logger, "[Controller]收到#添加或更新会员#请求");
        SellerInfo seller = (SellerInfo) session.getAttribute(Constants.LOGIN_FLAG_ADMIN);
        // 根据ID是否为空判断是新增还是编辑
        if (memberInfo.getId() == null) {
            LogUtil.info(logger, "[Controller]#添加会员#,memberInfo={0}", memberInfo);
            memberService.addMember(memberInfo);
            logService.record(LogSourceEnum.backstage, SubSystemTypeEnum.employee, seller.getSellerNo(), "增加会员" + memberInfo.getMemberNo());
        } else {
            LogUtil.info(logger, "[Controller]#修改会员#,memberInfo={0}", memberInfo);
            memberService.updateMember(memberInfo);
            logService.record(LogSourceEnum.backstage, SubSystemTypeEnum.employee, seller.getSellerNo(), "修改会员" + memberInfo.getMemberNo());
        }

        return ResultSet.success();
    }

    /**
     * 根据ID获取会员信息
     */
    @ResponseBody
    @RequestMapping(value = "/getById", method = RequestMethod.GET)
    public ResultSet getById(long id) {
        LogUtil.info(logger, "[Controller]收到#根据ID获取会员信息#请求,id={0}", id);

        MemberInfo memberInfo = memberService.queryMember(id);

        return ResultSet.success().put("memberInfo", memberInfo);
    }

    /**
     * 根据ID删除会员
     */
    @ResponseBody
    @RequestMapping(value = "/delById", method = RequestMethod.POST)
    public ResultSet delById(Long id, HttpSession session) {
        LogUtil.info(logger, "[Controller]收到#根据ID删除会员信息#请求,id={0}", id);
        SellerInfo seller = (SellerInfo) session.getAttribute(Constants.LOGIN_FLAG_ADMIN);
        memberService.deleteMember(id);
        logService.record(LogSourceEnum.backstage, SubSystemTypeEnum.employee, seller.getSellerNo(), "删除会员" + id);
        return ResultSet.success();
    }

    /**
     * 修改会员积分值
     * @param moneyStr 变动金额,单位:元
     */
    @ResponseBody
    @RequestMapping(value = "/updateIntegral", method = RequestMethod.POST)
    public ResultSet updateIntegral(Long memberId, String moneyStr, HttpSession session) {
        LogUtil.info(logger, "[Controller]收到#会员积分变动#请求,id={0},moneyStr={1}", memberId, moneyStr);
        SellerInfo seller = (SellerInfo) session.getAttribute(Constants.LOGIN_FLAG_ADMIN);
        memberService.updateIntegral(memberId, moneyStr);
        logService.record(LogSourceEnum.backstage, SubSystemTypeEnum.employee, seller.getSellerNo(), "修改会员积分" + memberId);
        return ResultSet.success();
    }

    /**
     * 会员分析页面
     */
    @GetMapping("/analysis")
    public String analysis() {
        return "backstage/_member-analysis";
    }

    /**
     * 会员分析
     */
    @ResponseBody
    @GetMapping(value = "/getRankAndCounts")
    public ResultSet getRankAndCounts() {
        JSONArray rankAndCounts = memberService.getRankAndCounts();
        return ResultSet.success().put("rankAndCounts", rankAndCounts);
    }

    /****************************会员等级相关接口****************************/

    @GetMapping("/rank")
    public String rankList() {
        return "backstage/_member-rank-list";
    }

    /**
     * 分页查询会员等级信息
     */
    @ResponseBody
    @RequestMapping("/rank/list")
    public ResultSet queryRankList(MemberRankQueryRequest request) {
        PageInfo<MemberRank> memberRanks = memberService.listRank(request);
        return ResultSet.success().put("page", memberRanks);
    }

    /**
     * 查询所有会员等级信息
     */
    @ResponseBody
    @GetMapping("/rank/listAll")
    public ResultSet queryAllRankList() {
        List<MemberRank> memberRanks = memberService.listAllRank();
        return ResultSet.success().put("memberRanks", memberRanks);
    }

    /**
     * 增加会员等级信息
     */
    @ResponseBody
    @RequestMapping(value = "/rank/addOrUpdate", method = RequestMethod.POST)
    public ResultSet addMemRank(MemberRank rank) {
        // 根据ID是否为空判断是新增还是编辑
        if (rank.getId() == null) {
            memberService.addMemRank(rank);
        } else {
            memberService.updateMemRank(rank);
        }
        return ResultSet.success();
    }

    /**
     * 删除会员等级信息
     */
    @ResponseBody
    @RequestMapping(value = "/rank/delete", method = RequestMethod.POST)
    public ResultSet deleteMemRank(Long id) {
        int ret = memberService.deleteMemRank(id);
        return ResultSet.success().put("ret", ret);
    }

    /**
     * 根据id查询会员等级
     */
    @ResponseBody
    @RequestMapping(value = "/rank/getById", method = RequestMethod.POST)
    public ResultSet queryMemRank(Long id) {
        MemberRank memberRank = memberService.queryMemRank(id);
        return ResultSet.success().put("memberRank", memberRank);
    }

    /****************************会员积分方式相关接口****************************/

    /**
     * 会员积分策略页面
     */
    @GetMapping(value = "/integral")
    public String integral() {
        return "backstage/_member-integral";
    }

    /**
     * 查询会员积分策略（策略只有一条记录）
     */
    @ResponseBody
    @GetMapping(value = "/integral/query")
    public ResultSet queryMemIntegral() {
        MemberIntegral memberIntegral = memberService.queryMemIntegral();
        return ResultSet.success().put("memberIntegral", memberIntegral);
    }

    /**
     * 修改会员积分策略
     */
    @ResponseBody
    @PostMapping(value = "/integral/update")
    public ResultSet updateMemIntegral(MemberIntegral integral) {
        int ret = memberService.updateMemIntegral(integral);
        return ResultSet.success().put("ret", ret);
    }

    /****************************会员充值相关接口****************************/

    /**
     * 充值明细页面
     */
    @GetMapping(value = "/recharge")
    public String queryRecharge() {
        return "backstage/_member-recharge-list";
    }

    /**
     * 充值明细
     */
    @ResponseBody
    @PostMapping(value = "/recharge/list")
    public ResultSet queryRechargeDetail(MemberRechargeQueryRequest request) {
        PageInfo<MemberRechargeDetail> rechargeDetails = memberRechargeService.query(request);
        return ResultSet.success().put("page", rechargeDetails);
    }

    /**
     * 储值卡对账页面
     */
    @GetMapping(value = "/recharge/check/list")
    public String queryRechargeCheckPage() {
        return "backstage/_member-recharge-check";
    }

    /**
     * 储值卡对账
     */
    @ResponseBody
    @PostMapping(value = "/recharge/check")
    public ResultSet queryRechargeCheck(MemberRechargeCheckQueryRequest request) {
        List<RechargeCheckDetail> checkDetails = memberRechargeService.list(request);
        return ResultSet.success().put("checkDetails", checkDetails);
    }

}
