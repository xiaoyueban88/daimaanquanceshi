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

package edp.davinci.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.druid.util.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class HomeController {

    @Value("${baize.web.url}")
    private String bazeWebUrl;

    @RequestMapping("swagger")
    public String swagger() {
        return "redirect:swagger-ui.html";
    }

    @RequestMapping(value = {"", "/"})
    public void index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Object nextPage = session.getAttribute("nextPage");
        if (nextPage != null && !StringUtils.isEmpty(nextPage.toString())) {
            session.removeAttribute("nextPage");
            response.sendRedirect(nextPage.toString());
        } else {
            response.sendRedirect(bazeWebUrl);
        }
    }

    @RequestMapping("share/")
    public String shareIndex() {
        return "share";
    }
}
