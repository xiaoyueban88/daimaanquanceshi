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

package edp.davinci.model;

import java.util.List;

import com.google.common.collect.Lists;

import edp.core.consts.Consts;
import edp.core.model.RecordInfo;
import edp.core.utils.CollectionUtils;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class Widget extends RecordInfo<Widget> {
    private Long id;

    private String name;

    private String description;

    private Long viewId;

    private Long projectId;

    private Long type = 1L;

    private Boolean publish = false;

    private String config;

    /**
     * 兼容widgetContainer的dispose属性
     */
    private String dispose;

    private Integer widgetType = Consts.NORMAL_WIDGET_TYPE;

    private Long folderId;

    private String folderName;

    private Boolean isUsed = false;


    public static List<Widget> contertWidgets(List<WidgetContainer> widgetContainers) {
        List<Widget> widgets = Lists.newArrayList();
        if (CollectionUtils.isEmpty(widgetContainers)) {
            return widgets;
        }
        for (WidgetContainer widgetContainer : widgetContainers) {
            Widget widget = convertWidget(widgetContainer);
            widgets.add(widget);
        }
        return widgets;
    }

    public static Widget convertWidget(WidgetContainer widgetContainer) {
        Widget widget = new Widget();
        BeanUtils.copyProperties(widgetContainer, widget);
        widget.setWidgetType(1);
        return widget;
    }

}