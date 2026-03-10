package edp.davinci.dto.CollectDto;

import java.util.List;

import edp.davinci.model.CollectPaper;
import edp.davinci.model.CollectPaperTopic;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/9
 */
@Data
public class CollectPaperDto {

    private CollectPaper collectPaper;

    private List<CollectPaperTopic> topics;
}
