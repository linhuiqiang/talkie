package com.meta.controller.accountmanage;

import com.meta.BaseControllerUtil;
import com.meta.Result;
import com.meta.ServiceUrls;
import com.meta.VersionNumbers;
import com.meta.datetime.DateTimeUtil;
import com.meta.error.errorMsgDict;
import com.meta.model.merchant.MerchantEvent;
import com.meta.model.qmanage.QUser;
import com.meta.model.user.User;
import com.meta.regex.RegexUtil;
import com.meta.remark.remarkDict;
import com.meta.service.accountmanage.EnterPriseAccountService;
import com.meta.service.enterprise.EnterpriseEventService;
import com.meta.service.merchant.MerchantEventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.InetAddress;
import java.util.Date;

/**
 * Created by lhq on 2017/11/17.
 */
@RestController
@RequestMapping(VersionNumbers.NEW_SERVICE_VERSIONS)
@Api(value = "enter_prise_account", description = "企业账号管理信息接口")
public class EnterPriseAccountController extends BaseControllerUtil {

    @Autowired
    private EnterPriseAccountService enterPriseAccountService;
    @Autowired
    private MerchantEventService merchantEventService;
    @Autowired
    EnterpriseEventService enterpriseEventService;


    @RequestMapping(value = ServiceUrls.EnterPriseAccount.ENTER_PRISE_ACCOUNTS, method = RequestMethod.POST)
    @ApiOperation(value = "创建/修改企业信息", notes = "创建/修改企业信息")
    public Result<User> create(
            @ApiParam(name = "json_model", value = "", defaultValue = "")
            @RequestBody @Valid User user) throws Exception {
        User userParent = enterPriseAccountService.findOne(getUserId());
        if (RegexUtil.isNull(user.getId())) {
            Long countAccount = enterPriseAccountService.countByParentId(getUserId(), "9", "8");
            //（代理啊子代理企业 ）  的账号管理，最多只能建20个，
            if (countAccount >= 20) {
                return error(errorMsgDict.ADD_ACCOUNT_TO_LIMIT);
            }
            User userTemp = enterPriseAccountService.findByAccount(user.getAccount());
            if (!RegexUtil.isNull(userTemp)) {
                return error("该账号已存在，请重新填写！");
            }
            user.setParentId(getUserId());
            user.setIsDel(1);

            /**
             * 统计表
             */
            if (user.getMerchantLevel().equals("13")) {
                /**
                 * Q币余额
                 */
                QUser qUser = new QUser();
                qUser.setAlreadyBalance(0D);
                qUser.setCreateDate(DateTimeUtil.simpleDateTimeFormat(new Date()));
                qUser.setBalance(0D);
                qUser.setModifyDate(DateTimeUtil.simpleDateTimeFormat(new Date()));
                qUser.setUser(user);
                user.setqUser(qUser);

            }


            /**
             *  操作记录
             */
            MerchantEvent merchantEvent = new MerchantEvent();
            merchantEvent.setByUserAccount(user.getAccount());
            merchantEvent.setType(3);
            merchantEvent.setCreateDate(DateTimeUtil.simpleDateTimeFormat(new Date()));
            merchantEvent.setCreateUser(userParent.getId());
            if (user.getLanguage().equals("zh")) {
                merchantEvent.setRemark(remarkDict.ADD + remarkDict.USER + user.getName());
            } else if (user.getLanguage().equals("en")) {
                merchantEvent.setRemark(remarkDict.ADD_ENGLISHI + "  " + remarkDict.USER_ENGLISH + "  " + user.getName());
            }

            merchantEvent.setIp(InetAddress.getLocalHost().getHostAddress());
            merchantEvent.setUser(userParent);
            merchantEventService.save(merchantEvent);
            //企业操作记录
            if (userParent.getMerchantLevel().equals("4")) enterpriseEventService.UserEventLog(userParent, user, 13);

            return success(enterPriseAccountService.save(user));

        } else {
            /**
             * 操作记录
             */

            MerchantEvent merchantEvent = new MerchantEvent();
            merchantEvent.setByUserAccount(user.getAccount());
            merchantEvent.setCreateDate(DateTimeUtil.simpleDateTimeFormat(new Date()));
            merchantEvent.setType(5);
            merchantEvent.setCreateUser(userParent.getId());
            if (user.getLanguage().equals("zh")) {
                merchantEvent.setRemark(remarkDict.MODIFY + remarkDict.USER + user.getName());
            } else if (user.getLanguage().equals("en")) {
                merchantEvent.setRemark(remarkDict.MODIFY_ENGLISH + "  " + remarkDict.USER_ENGLISH + "  " + user.getName());
            }
            merchantEvent.setIp(getIpAddr());
            merchantEvent.setUser(userParent);
            merchantEventService.save(merchantEvent);
            //企业操作记录
            if (userParent.getMerchantLevel().equals("4")) enterpriseEventService.UserEventLog(userParent, user, 15);
            return success(enterPriseAccountService.save(user));
        }
    }

    @RequestMapping(value = ServiceUrls.EnterPriseAccount.ENTER_PRISE_ACCOUNTS, method = RequestMethod.GET)
    @ApiOperation(value = "获取企业(账号管理)列表", notes = "根据查询条件获企业(账号管理)列表在前端表格展示")
    public Result<User> search(
            @ApiParam(name = "filters", value = "过滤器，为空检索所有条件", defaultValue = "")
            @RequestParam(value = "filters", required = false) String filters,
            @ApiParam(name = "sorts", value = "排序，规则参见说明文档", defaultValue = "")
            @RequestParam(value = "sorts", required = false) String sorts,
            @ApiParam(name = "size", value = "分页大小", defaultValue = "15")
            @RequestParam(value = "size", required = false) int size,
            @ApiParam(name = "page", value = "页码", defaultValue = "1")
            @RequestParam(value = "page", required = false) int page) {
        Page<User> result = enterPriseAccountService.searchExtendDistinct(filters, sorts, page, size);
        return getResultList(result.getContent(), result.getTotalElements(), page, size);
    }

    @RequestMapping(value = ServiceUrls.EnterPriseAccount.ENTER_PRISE_ACCOUNT, method = RequestMethod.POST)
    @ApiOperation(value = "根据ID修改密码", notes = "根据ID修改密码")
    public Result modifyPassword(
            @ApiParam(name = "id", value = "id", defaultValue = "")
            @RequestParam(value = "id", required = false) Long id,
            @ApiParam(name = "password", value = "password", defaultValue = "")
            @RequestParam(value = "password", required = false) String password) throws Exception {
        enterPriseAccountService.modifyPasswordById(id, password);
        return success("");
    }

    @RequestMapping(value = ServiceUrls.EnterPriseAccount.ENTER_PRISE_ACCOUNT_MODIFY_STATUS_BY_ID, method = RequestMethod.POST)
    @ApiOperation(value = "根据ID变更状态", notes = "根据ID变更状态")
    public Result modifyStatusById(
            @ApiParam(name = "id", value = "id", defaultValue = "")
            @RequestParam(value = "id", required = false) Long id,
            @ApiParam(name = "status", value = "status", defaultValue = "")
            @RequestParam(value = "status", required = false) String status) throws Exception {
        enterPriseAccountService.modifyStatusById(id, status);
        return success("");
    }

    @RequestMapping(value = ServiceUrls.EnterPriseAccount.ENTER_PRISE_ACCOUNTS, method = RequestMethod.DELETE)
    @ApiOperation(value = "根据ID删除账号", notes = "根据ID删除账号")
    public Result delete(
            @ApiParam(name = "id", value = "id", defaultValue = "")
            @RequestParam(value = "id") Long id) throws Exception {
        User userParent = enterPriseAccountService.findOne(getUserId());
        User user = enterPriseAccountService.findOne(id);
        enterpriseEventService.UserEventLog(userParent, user, 14);
        enterPriseAccountService.delete(id);
        return success(null);
    }


}
