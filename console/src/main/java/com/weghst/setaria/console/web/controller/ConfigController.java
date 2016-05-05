package com.weghst.setaria.console.web.controller;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.weghst.setaria.console.web.Constants;
import com.weghst.setaria.console.web.ErrorCodes;
import com.weghst.setaria.console.web.ErrorResult;
import com.weghst.setaria.console.web.Result;
import com.weghst.setaria.core.ObjectMapperUtils;
import com.weghst.setaria.core.domain.App;
import com.weghst.setaria.core.domain.Config;
import com.weghst.setaria.core.domain.Env;
import com.weghst.setaria.core.domain.User;
import com.weghst.setaria.core.dto.ConfigDto;
import com.weghst.setaria.core.service.AppNotFoundException;
import com.weghst.setaria.core.service.AppService;
import com.weghst.setaria.core.service.ConfigService;

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Controller
@RequestMapping("/configs")
public class ConfigController {

    @Autowired
    private AppService appService;
    @Autowired
    private ConfigService configService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public Object save(@RequestBody Config config, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER_ATTR_NAME);
        configService.save(config, user.getEmail());
        return Result.SUCCESS;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public Object update(@PathVariable int id, @RequestBody Config config, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER_ATTR_NAME);
        config.setId(id);
        configService.update(config, user.getEmail());
        return Result.SUCCESS;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object delete(@PathVariable int id, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER_ATTR_NAME);
        configService.delete(id, user.getEmail());
        return Result.SUCCESS;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Object get(@PathVariable int id) {
        return configService.findById(id);
    }

    /**
     * @param appName
     * @param appEnv
     * @return
     */
    @RequestMapping(value = "/pull/{appName}/{appEnv}", method = RequestMethod.GET)
    public @ResponseBody Object pull(@PathVariable String appName, @PathVariable String appEnv) {
        Env env;
        try {
            env = Env.valueOf(appEnv);
        } catch (IllegalArgumentException e) {
            ErrorResult result = new ErrorResult(ErrorCodes.E_20001);
            result.setErrorMessage("错误的应用环境 [" + appEnv + "], 环境可选值为 " + Arrays.toString(Env.values()));
            return ResponseEntity.badRequest().body(result);
        }

        try {
            return configService.findByAppNameAndEnv(appName, env);
        } catch (AppNotFoundException e) {
            ErrorResult result = new ErrorResult(ErrorCodes.E_20002);
            result.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @RequestMapping(value = "export/{appId}", method = RequestMethod.GET)
    public void export(@PathVariable int appId, HttpServletResponse response) throws IOException {
        List<Config> list = configService.findByAppId(appId);
        List<ConfigDto> configDtos = new ArrayList<>(list.size());
        for (Config c : list) {
            ConfigDto configDto = new ConfigDto();
            BeanUtils.copyProperties(c, configDto);
            configDtos.add(configDto);
        }

        App app = appService.findById(appId);
        String filename = app.getName() + "-" + app.getEnv() + "-" + app.getLastUpdatedTime() + ".json";
        byte[] bytes = ObjectMapperUtils.writeValueAsBytes(configDtos);

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        response.setContentLength(bytes.length);

        response.getOutputStream().write(bytes);
    }

    @RequestMapping("/add.v")
    public ModelAndView addView() {
        ModelAndView mav = new ModelAndView("config-add");
        return mav;
    }

    @RequestMapping("/edit.v")
    public ModelAndView editView(int id) {
        ModelAndView mav = new ModelAndView("config-edit");
        return mav;
    }

    @RequestMapping("/details.v")
    public ModelAndView detailsView(int id) {
        ModelAndView mav = new ModelAndView("config-details");
        mav.addObject("config", configService.findById(id));
        return mav;
    }

    @RequestMapping("/list.v")
    public ModelAndView listView(int appId) {
        ModelAndView mav = new ModelAndView("config-list");
        mav.addObject("app", appService.findById(appId));
        mav.addObject("configs", configService.findByAppId(appId));
        return mav;
    }
}
