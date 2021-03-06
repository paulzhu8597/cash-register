/**
 * Cash-Register
 * Copyright (c) 1995-2018 All Rights Reserved.
 */
package cn.cash.register.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import cn.cash.register.common.request.MemberInfoQueryRequest;
import cn.cash.register.common.request.MemberRankQueryRequest;
import cn.cash.register.dao.MemberInfoMapper;
import cn.cash.register.dao.MemberIntegralMapper;
import cn.cash.register.dao.MemberRankMapper;
import cn.cash.register.dao.domain.MemberInfo;
import cn.cash.register.dao.domain.MemberIntegral;
import cn.cash.register.dao.domain.MemberRank;
import cn.cash.register.dao.domain.MemberRankAndCounts;
import cn.cash.register.service.MemberService;
import cn.cash.register.util.LogUtil;
import cn.cash.register.util.Money;

/**
 * 会员服务接口实现类
 * @author HuHui
 * @version $Id: MemberServiceImpl.java, v 0.1 2018年4月20日 上午11:42:21 HuHui Exp $
 */
@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger  logger = LoggerFactory.getLogger(MemberServiceImpl.class);

    @Resource
    private MemberInfoMapper     infoMapper;

    @Resource
    private MemberRankMapper     rankMapper;

    @Resource
    private MemberIntegralMapper integralMapper;

    @Resource
    private TransactionTemplate  txTemplate;

    /****************************会员信息相关接口****************************/

    @Override
    public Long addMember(MemberInfo memberInfo) {
        LogUtil.info(logger, "收到增加会员信息请求");
        memberInfo.setGmtCreate(new Date());
        return infoMapper.insertSelective(memberInfo);
    }

    @Override
    public int deleteMember(Long memberId) {
        LogUtil.info(logger, "收到删除会员信息请求,memberId={0}", memberId);
        return infoMapper.deleteByPrimaryKey(memberId);
    }

    @Override
    public int updateMember(MemberInfo memberInfo) {
        LogUtil.info(logger, "收到修改会员信息请求,id={0}", memberInfo.getId());
        return infoMapper.updateByPrimaryKeySelective(memberInfo);
    }

    @Override
    public MemberInfo queryMember(Long id) {
        LogUtil.info(logger, "收到查询会员信息请求,id={0}", id);
        return infoMapper.selectByPrimaryKey(id);
    }

    @Override
    public MemberInfo queryMemberByMemberNo(String memberNo) {
        LogUtil.info(logger, "收到查询会员信息请求,memberNo={0}", memberNo);
        if (StringUtils.isBlank(memberNo)) {
            return null;
        }
        return infoMapper.selectByNo(memberNo);
    }

    @Override
    public List<MemberInfo> search(String keyword) {
        LogUtil.info(logger, "收到搜索会员信息请求,keyword={0}", keyword);
        return infoMapper.search(keyword);
    }

    @Override
    public PageInfo<MemberInfo> queryList(MemberInfoQueryRequest request) {
        LogUtil.info(logger, "收到分页查询会员请求");
        request.validate();
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        PageHelper.orderBy(request.getSidx() + " " + request.getOrder());

        List<MemberInfo> list = infoMapper.list(request);

        return new PageInfo<MemberInfo>(list);
    }

    @Override
    public void updateIntegral(Long memberId, String moneyStr) {
        if (!NumberUtils.isDigits(moneyStr)) {
            return;
        }
        updateIntegral(memberId, new Money(moneyStr));
    }

    @Override
    public void updateIntegral(Long memberId, Money money) {
        LogUtil.info(logger, "收到修改会员积分请求,memberId={0},money={1}", memberId, money);

        if (memberId == null) {
            return;
        }

        try {
            //计算积分值
            MemberIntegral memIntegral = queryMemIntegral();
            if (memIntegral == null || !memIntegral.getStatus()) {
                return;
            }

            double integralValue = money.getAmount().doubleValue() / memIntegral.getIntegralValue();

            //查询会员信息并修改
            MemberInfo member = infoMapper.selectByPrimaryKey(memberId);

            if (member == null || !member.getStatus()) {//不启用
                return;
            }

            double newValue = member.getMemberIntegral() + integralValue;
            member.setMemberIntegral(newValue);

            //会员自动升级
            List<MemberRank> memberRanks = rankMapper.listAll();
            for (MemberRank rank : memberRanks) {
                if (rank.getIsIntegral() && rank.getIsAutoUpgrade()) {
                    if (member.getMemberIntegral() >= rank.getIntegralToUpgrade()) {
                        member.setMemberRank(rank.getRankTitle());
                        member.setMemberDiscount(rank.getDiscount());
                    }
                }
            }

            infoMapper.updateByPrimaryKeySelective(member);
        } catch (Exception e) {
            LogUtil.error(e, logger, "修改会员积分异常");
        }
    }

    @Override
    public JSONArray getRankAndCounts() {
        List<MemberRankAndCounts> items = infoMapper.groupByRank();
        JSONArray jsonArray = new JSONArray();
        for (MemberRankAndCounts item : items) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("memberRank", item.getMemberRank());
            jsonObj.put("counts", item.getCounts());
            jsonArray.add(jsonObj);
        }

        return jsonArray;
    }

    /****************************会员等级相关接口****************************/

    @Override
    public Long addMemRank(MemberRank rank) {
        LogUtil.info(logger, "收到增加会员等级请求");
        rank.setGmtCreate(new Date());
        return rankMapper.insertSelective(rank);
    }

    @Override
    public int deleteMemRank(Long id) {
        LogUtil.info(logger, "收到删除会员等级请求,id={0}", id);
        return rankMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int updateMemRank(MemberRank rank) {
        LogUtil.info(logger, "收到修改会员等级请求,id={0}", rank.getId());
        return rankMapper.updateByPrimaryKeySelective(rank);
    }

    @Override
    public MemberRank queryMemRank(Long id) {
        LogUtil.info(logger, "收到查询会员等级请求,id={0}", id);
        return rankMapper.selectByPrimaryKey(id);
    }

    @Override
    public PageInfo<MemberRank> listRank(MemberRankQueryRequest request) {
        LogUtil.info(logger, "收到翻页查询会员等级请求");
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        PageHelper.orderBy(request.getSidx() + " " + request.getOrder());
        return new PageInfo<MemberRank>(rankMapper.listAll());
    }

    @Override
    public List<MemberRank> listAllRank() {
        LogUtil.info(logger, "收到查询所有会员等级请求");
        return rankMapper.listAll();
    }

    /****************************会员积分方式相关接口****************************/

    @Override
    public MemberIntegral queryMemIntegral() {
        LogUtil.info(logger, "收到查询会员积分方式请求");
        return integralMapper.selectByPrimaryKey(1L);//只有一条记录
    }

    @Override
    public int updateMemIntegral(MemberIntegral integral) {
        LogUtil.info(logger, "收到修改会员积分方式请求");
        int result = integralMapper.updateByPrimaryKeySelective(integral);
        if (result == 0) {//没有id为1的记录
            integral.setId(1L);//只有一条记录
            integral.setStatus(true);
            integral.setExchangeType(false);
            integral.setIntegralType(true);
            return integralMapper.insert(integral);
        }
        return result;
    }

    /****************************会员导入相关接口****************************/
    @Override
    public List<MemberInfo> transfer2MemberInfo(List<List<String>> excelData) {
        List<MemberInfo> result = new ArrayList<MemberInfo>();
        try {
            for (List<String> _rowData : excelData) {
                MemberInfo memberInfo = new MemberInfo();
                memberInfo.setMemberNo(StringUtils.isBlank(_rowData.get(0)) ? null : _rowData.get(0));
                memberInfo.setMemberName(StringUtils.isBlank(_rowData.get(1)) ? null : _rowData.get(1));
                memberInfo.setMemberRank(StringUtils.isBlank(_rowData.get(2)) ? null : _rowData.get(2));
                memberInfo.setMemberDiscount(StringUtils.isBlank(_rowData.get(3)) ? null : Integer.valueOf(_rowData.get(3)));
                memberInfo.setMemberIntegral(StringUtils.isBlank(_rowData.get(4)) ? null : Double.valueOf(_rowData.get(4)));
                memberInfo.setPhone(StringUtils.isBlank(_rowData.get(5)) ? null : _rowData.get(5));
                memberInfo.setPassword(StringUtils.isBlank(_rowData.get(6)) ? null : _rowData.get(6));
                memberInfo.setBirthday(StringUtils.isBlank(_rowData.get(7)) ? null : _rowData.get(7));
                memberInfo.setIsOnCredit(StringUtils.isBlank(_rowData.get(8)) ? null : Boolean.valueOf(_rowData.get(8)));
                memberInfo.setQqNo(StringUtils.isBlank(_rowData.get(9)) ? null : _rowData.get(9));
                memberInfo.setEmail(StringUtils.isBlank(_rowData.get(10)) ? null : _rowData.get(10));
                memberInfo.setAddress(StringUtils.isBlank(_rowData.get(11)) ? null : _rowData.get(11));
                memberInfo.setShopperName(StringUtils.isBlank(_rowData.get(12)) ? null : _rowData.get(12));
                memberInfo.setRemark(StringUtils.isBlank(_rowData.get(13)) ? null : _rowData.get(13));
                memberInfo.setStatus(StringUtils.isBlank(_rowData.get(14)) ? null : Boolean.valueOf(_rowData.get(14)));
                result.add(memberInfo);
            }
        } catch (Exception e) {
            LogUtil.error(e, logger, "导入会员信息格式有误");
            throw new RuntimeException("导入会员信息格式有误", e);
        }
        return result;
    }

}
