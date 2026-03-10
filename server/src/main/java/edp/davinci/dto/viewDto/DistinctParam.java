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

package edp.davinci.dto.viewDto;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import edp.core.exception.NotFoundException;
import edp.core.utils.AESUtils;
import edp.core.utils.CollectionUtils;
import edp.davinci.common.utils.EncryptUtil;
import edp.davinci.common.utils.StringUtil;
import lombok.Data;

@Data
@NotNull(message = "request parameter cannot be null")
public class DistinctParam {
    @NotEmpty(message = "distinct column cannot be EMPTY")
    private List<String> columns;

    private List<String> filters;

    private List<Param> params;

    private String direction = "ASC";

    private Boolean cache;

    private Long expired;

    private String type = "distinct";

    private String orderColumn;

    private String nonce;

    private Long t;

    private String sign;

    public static void decryParams(DistinctParam distinctParam) {
        List<Param> params = distinctParam.getParams();
        if (!CollectionUtils.isEmpty(params)) {
            List<Param> paramList = Lists.newArrayList();
            params.forEach(p -> {
                String value = p.getValue();
                if (p.getEncry() != null && p.getEncry() == 1) {
                    if (!StringUtils.isEmpty(value)) {
                        String decrypt = AESUtils.decrypt(value.replace("'", ""), null);
                        if (decrypt == null) {
                            throw new NotFoundException("param is null");
                        }
                        p.setValue(decrypt);
                    }
                }
                paramList.add(p);
            });
            distinctParam.setParams(paramList);
        }
    }

    public boolean checkSign() {
        // 校验时间戳
        long time = new Date().getTime();
        // 不超过5分钟
        if(Math.abs(time - t) > 5 * 60 * 1000) {
            return false;
        }
        JSONObject object = new JSONObject();
        object.put("columns", columns);
        object.put("filters", filters);
        object.put("params", params);
        String paramString = StringUtil.getNormalString(JSON.toJSONString(object));
        String encodeParam = EncryptUtil.encodeSHA(paramString+nonce+t.toString());
        return encodeParam.equals(sign);
    }
}
