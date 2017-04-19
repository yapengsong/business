package com.eayun.database.backup.ecmccontroller;

import com.alibaba.fastjson.JSONObject;
import com.eayun.common.ConstantClazz;
import com.eayun.common.dao.ParamsMap;
import com.eayun.common.dao.QueryMap;
import com.eayun.common.dao.support.Page;
import com.eayun.common.filter.SessionUserInfo;
import com.eayun.database.backup.bean.BackupCategory;
import com.eayun.database.backup.model.CloudRDSBackup;
import com.eayun.database.backup.service.EcmcRDSBackupService;
import com.eayun.log.ecmcsevice.EcmcLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * ECMC云数据库备份Controller
 *
 * @author fan.zhang
 */
@Controller
@RequestMapping("/ecmc/rds/backup")
public class EcmcRDSBackupController {

    public static final Logger log = LoggerFactory.getLogger(EcmcRDSBackupController.class);

    @Autowired
    private EcmcRDSBackupService rdsBackupService;
    @Autowired
    private EcmcLogService logService;

    @RequestMapping(value = "/getBackups", method = RequestMethod.POST)
    @ResponseBody
    public String listBackups(HttpServletRequest request, Page page, @RequestBody ParamsMap map) throws Exception {
        log.info("分页查询备份列表开始");
        SessionUserInfo sessionUser = (SessionUserInfo) request.getSession().getAttribute(ConstantClazz.SYS_SESSION_USERINFO);

        int pageSize = map.getPageSize();
        int pageNumber = map.getPageNumber();

        QueryMap queryMap = new QueryMap();
        queryMap.setPageNum(pageNumber);
        queryMap.setCURRENT_ROWS_SIZE(pageSize);

        page = rdsBackupService.getBackups(page, map, sessionUser, queryMap);
        return JSONObject.toJSONString(page);
    }

    @RequestMapping(value = "/getBackupsByInstanceId", method = RequestMethod.POST)
    @ResponseBody
    public String getBackupsByInstanceId(HttpServletRequest request, Page page, @RequestBody ParamsMap map) throws Exception {
        log.info("分页查询指定实例的备份列表开始");

        int pageSize = map.getPageSize();
        int pageNumber = map.getPageNumber();

        QueryMap queryMap = new QueryMap();
        queryMap.setPageNum(pageNumber);
        queryMap.setCURRENT_ROWS_SIZE(pageSize);

        page = rdsBackupService.getBackupsByInstanceId(page, map, queryMap);
        return JSONObject.toJSONString(page);
    }

    @RequestMapping(value = "/createBackup", method = RequestMethod.POST)
    @ResponseBody
    public String createBackup(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("创建实例的手动备份开始");

        JSONObject resp = new JSONObject();
        String datacenterId = map.get("dcId") == null ? "" : map.get("dcId").toString();
        String projectId = map.get("prjId") == null ? "" : map.get("prjId").toString();
        String name = map.get("name") == null ? "" : map.get("name").toString();
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        String parentId = map.get("parentId") == null ? "" : map.get("parentId").toString();
        String category = BackupCategory.MANUAL;
        CloudRDSBackup cloudRDSBackup = new CloudRDSBackup();
        try {
            cloudRDSBackup = rdsBackupService.createBackup(datacenterId, projectId, name, instanceId, parentId, category);
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("message", "创建实例的手动备份成功");
            logService.addLog("创建手动备份",  ConstantClazz.LOG_TYPE_RDS, cloudRDSBackup.getInstanceName(), cloudRDSBackup.getProjectId(), 1, cloudRDSBackup.getInstanceId(),null);
        } catch (Exception e) {
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "创建实例的手动备份失败");
            if(StringUtils.isEmpty(cloudRDSBackup.getInstanceName())){
                String instanceName = rdsBackupService.getInstanceNameById(instanceId);
                cloudRDSBackup.setInstanceName(instanceName);
            }
            logService.addLog("创建手动备份",  ConstantClazz.LOG_TYPE_RDS, cloudRDSBackup.getInstanceName()==null?"":cloudRDSBackup.getInstanceName(), projectId, 0, instanceId,e);
            log.error("创建实例的备份失败", e);
            throw e;
        }
        return resp.toJSONString();
    }

    @RequestMapping(value = "/updateBackup", method = RequestMethod.POST)
    @ResponseBody
    public String updateBackup(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("修改备份的名称开始");

        JSONObject resp = new JSONObject();
        String backupId = map.get("backupId") == null ? "" : map.get("backupId").toString();
        String name = map.get("name") == null ? "" : map.get("name").toString();

        try {
            rdsBackupService.updateBackup(backupId, name);
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("message", "修改备份名称成功");
        } catch (Exception e) {
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "修改备份名称失败");
            log.error("修改备份名称失败", e);
            throw e;
        }
        return resp.toJSONString();
    }

    @RequestMapping(value = "/deleteBackup", method = RequestMethod.POST)
    @ResponseBody
    public String deleteBackup(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("删除备份开始");

        JSONObject resp = new JSONObject();
        String backupId = map.get("backupId") == null ? "" : map.get("backupId").toString();
        CloudRDSBackup cloudRDSBackup = new CloudRDSBackup();
        try {
            Map<String, Object> result = rdsBackupService.deleteBackup(backupId);
            cloudRDSBackup = (CloudRDSBackup) result.get("rdsBackup");
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("message", "删除备份成功");
            logService.addLog("删除手动备份",  ConstantClazz.LOG_TYPE_RDS, cloudRDSBackup.getInstanceName(), cloudRDSBackup.getProjectId(), 1, cloudRDSBackup.getInstanceId() ,null);
        } catch (Exception e) {
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "删除备份失败");
            logService.addLog("删除手动备份",  ConstantClazz.LOG_TYPE_RDS, cloudRDSBackup.getInstanceName()==null?"":cloudRDSBackup.getInstanceName(),
                    cloudRDSBackup.getProjectId()==null?"":cloudRDSBackup.getProjectId(), 0, cloudRDSBackup.getInstanceId()==null?"":cloudRDSBackup.getInstanceId() ,e);
            log.error("删除备份失败", e);
            throw e;
        }
        return resp.toJSONString();
    }

    @RequestMapping(value = "/enableAutomaticBackup", method = RequestMethod.POST)
    @ResponseBody
    public String enableAutomaticBackup(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("启用自动备份开始");

        JSONObject resp = new JSONObject();
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        Map<String,String> info = rdsBackupService.getInfoForLog(instanceId);
        String instanceName = info.get("instanceName");
        String projectId = info.get("projectId");
        try {
            int rows = rdsBackupService.enableAutomaticBackup(instanceId);
            if (rows == 1) {
                resp.put("respCode", ConstantClazz.SUCCESS_CODE);
                resp.put("message", "开启自动备份成功");
                logService.addLog("开启自动备份",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 1, instanceId, null);
            } else {
                resp.put("respCode", ConstantClazz.ERROR_CODE);
                resp.put("message", "开启自动备份失败");
                logService.addLog("开启自动备份",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 0, instanceId, null);
            }
        } catch (Exception e) {
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "开启自动备份失败");
            logService.addLog("开启自动备份",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 0, instanceId, e);
            log.error("开启自动备份失败", e);
            throw e;
        }
        return resp.toJSONString();
    }

    @RequestMapping(value = "/disableAutomaticBackup", method = RequestMethod.POST)
    @ResponseBody
    public String disableAutomaticBackup(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("停用自动备份开始");

        JSONObject resp = new JSONObject();
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        Map<String,String> info = rdsBackupService.getInfoForLog(instanceId);
        String instanceName = info.get("instanceName");
        String projectId = info.get("projectId");
        try {
            rdsBackupService.disableAutomaticBackup(instanceId);
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("message", "停用自动备份成功");
            logService.addLog("关闭自动备份",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 1,instanceId ,null);
        } catch (Exception e) {
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "停用自动备份失败");
            logService.addLog("关闭自动备份",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 0,instanceId ,e);
            log.error("停用自动备份失败", e);
            throw e;
        }
        return resp.toJSONString();
    }

    @RequestMapping(value = "/verifyBackupName", method = RequestMethod.POST)
    @ResponseBody
    public boolean verifyBackupName(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("检查数据中心-项目下备份名称重复开始");
        String datacenterId = map.get("datacenterId") == null ? "" : map.get("datacenterId").toString();
        String projectId = map.get("projectId") == null ? "" : map.get("projectId").toString();
        String backupName = map.get("backupName") == null ? "" : map.get("backupName").toString();
        return rdsBackupService.verifyBackupName(datacenterId, projectId, backupName);
    }

    @RequestMapping(value = "/getCurrentManualBackupCount", method = RequestMethod.POST)
    @ResponseBody
    public String getCurrentManualBackupCount(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("查询实例当前手动备份数量开始");
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        long count = 0l;
        try {
            count = rdsBackupService.getCurrentManualBackupCount(instanceId);
        } catch (Exception e) {
            log.error("查询实例当前手动备份数量", e);
            throw e;
        }
        return JSONObject.toJSONString(count);
    }

    @RequestMapping(value = "/getMaxManualBackupCount", method = RequestMethod.POST)
    @ResponseBody
    public String getMaxManualBackupCount(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("查询项目手动备份数量配额开始");
        String projectId = map.get("projectId") == null ? "" : map.get("projectId").toString();
        long count = 0l;
        try {
            count = rdsBackupService.getMaxManualBackupCount(projectId);
        } catch (Exception e) {
            log.error("查询项目手动备份数量配额", e);
            throw e;
        }
        return JSONObject.toJSONString(count);
    }

    @RequestMapping(value = "/getAutoBackupEnableStatus", method = RequestMethod.POST)
    @ResponseBody
    public String getAutoBackupEnableStatus(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("查询实例自动备份开启状态开始");
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        JSONObject resp = new JSONObject();
        try {
            Map<String, String> info = rdsBackupService.getAutoBackupEnableStatus(instanceId);
            boolean isAutoBackupEnabled = Boolean.valueOf(info.get("isAutoBackupEnabled"));
            String scheduleTime = info.get("scheduleTime");
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("isAutoBackupEnabled", isAutoBackupEnabled);
            resp.put("scheduleTime", scheduleTime);
            resp.put("message", "查询自动备份开启状态成功");
        } catch (Exception e) {
            log.error("查询实例自动备份开启状态失败", e);
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("isAutoBackupEnabled", false);
            resp.put("scheduleTime", null);
            resp.put("message", "查询自动备份开启状态失败");
            throw e;
        }
        return JSONObject.toJSONString(resp);
    }

    @RequestMapping(value = "/modifyScheduleTime", method = RequestMethod.POST)
    @ResponseBody
    public String modifyScheduleTime(HttpServletRequest request, @RequestBody Map map) throws Exception {
        log.info("修改实例自动备份时间开始");
        String instanceId = map.get("instanceId") == null ? "" : map.get("instanceId").toString();
        String scheduleTime = map.get("scheduleTime") == null ? "" : map.get("scheduleTime").toString();
        Map<String,String> info = rdsBackupService.getInfoForLog(instanceId);
        String instanceName = info.get("instanceName");
        String projectId = info.get("projectId");
        JSONObject resp = new JSONObject();
        try {
            rdsBackupService.modifyScheduleTime(instanceId, scheduleTime);
            resp.put("respCode", ConstantClazz.SUCCESS_CODE);
            resp.put("message", "修改实例自动备份时间成功");
            logService.addLog("修改自动备份时间",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 1,instanceId ,null);
        } catch (Exception e) {
            log.error("修改实例自动备份时间失败", e);
            resp.put("respCode", ConstantClazz.ERROR_CODE);
            resp.put("message", "修改实例自动备份时间失败");
            logService.addLog("修改自动备份时间",  ConstantClazz.LOG_TYPE_RDS, instanceName, projectId, 0,instanceId ,e);
            throw e;
        }
        return JSONObject.toJSONString(resp);
    }
}
