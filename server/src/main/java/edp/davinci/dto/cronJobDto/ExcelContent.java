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

package edp.davinci.dto.cronJobDto;

import java.io.File;

import edp.davinci.core.enums.FileTypeEnum;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class ExcelContent {
    private String name;
    private File file;
    private FileTypeEnum fileType;

    public ExcelContent(String name, String filePath) {
        this.name = name;
        this.file = new File(filePath);
        if (StringUtils.endsWith(filePath, FileTypeEnum.XLSM.getFormat())) {
            this.fileType = FileTypeEnum.XLSM;
        } else {
            this.fileType = FileTypeEnum.XLSX;
        }
    }
}
