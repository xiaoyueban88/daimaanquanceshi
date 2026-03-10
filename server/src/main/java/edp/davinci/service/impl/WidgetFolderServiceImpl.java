package edp.davinci.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.davinci.dao.WidgetContainerMapper;
import edp.davinci.dao.WidgetFolderMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dto.WidgetFolder.WidgetFolderDto;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetContainer;
import edp.davinci.model.WidgetFolder;
import edp.davinci.service.WidgetFolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/5/6
 */
@Service("widgetFolderService")
@Slf4j
public class WidgetFolderServiceImpl implements WidgetFolderService {

    @Autowired
    private WidgetFolderMapper widgetFolderMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetContainerMapper widgetContainerMapper;

    @Override
    public WidgetFolder create(WidgetFolder widgetFolder) throws UnAuthorizedException, ServerException {
        int insert = widgetFolderMapper.insert(widgetFolder);
        if (insert > 0) {
            return widgetFolder;
        } else {
            throw new ServerException("create widgetFolder fail");
        }
    }

    @Override
    public void update(WidgetFolder widgetFolder) throws UnAuthorizedException, ServerException {
        if (widgetFolder.getId() == null) {
            throw new ServerException("create widgetFolder fail, id is null");
        }
        int update = widgetFolderMapper.update(widgetFolder);
        if (update <= 0) {
            throw new ServerException("create widgetFolder fail");
        }
    }

    @Override
    @Transactional
    public void loopDeleteFolder(Long projectId, Long folderId) throws UnAuthorizedException, ServerException {
        Set<Long> ids = widgetFolderMapper.getFolderIdsByParentId(folderId);
        if (!CollectionUtils.isEmpty(ids)) {
            ids.forEach(id -> {
                loopDeleteFolder(projectId, id);
            });
        }

        // 将目录下的所有widget挂在根目录下
        List<Widget> widgets = widgetMapper.getByFolderId(projectId, folderId);
        widgets.forEach(widget -> {
            widget.setFolderId(0L);
        });
        if (!CollectionUtils.isEmpty(widgets)) {
            int update = widgetMapper.updateFolderIdBatch(widgets);
            if (update <= 0) {
                throw new ServerException("变更widget挂载目录失败");
            }
        }


        // 将目录下的所有widgetcontainer挂在根目录下
        List<WidgetContainer> widgetContainers = widgetContainerMapper.getByFolderId(projectId, folderId);
        widgetContainers.forEach(w -> {
            w.setFolderId(0L);
        });

        if (!CollectionUtils.isEmpty(widgetContainers)) {
            int updateContainer = widgetContainerMapper.updateFolderIdBatch(widgetContainers);
            if (updateContainer <= 0) {
                throw new ServerException("变更widgetcontainer挂载目录失败");
            }
        }

        // 删除文件夹
        int delete = widgetFolderMapper.delete(projectId, folderId);
        if (delete < 0) {
            throw new ServerException("删除目录失败");
        }
    }

    @Override
    public WidgetFolderDto getWidgetFolderDto(Long projectId) throws UnAuthorizedException, ServerException {
        // 获取项目下的所有文件夹信息
        List<WidgetFolder> widgetFolders = widgetFolderMapper.getWidgetFoldersByProjectId(projectId);

        // 初始化根目录
        WidgetFolderDto root = new WidgetFolderDto.Builder().id(0L).projectId(projectId).name("根目录").build();

        if (!CollectionUtils.isEmpty(widgetFolders)) {
            // parentId -> WidgetFolder集合
            Map<Long, Set<WidgetFolderDto>> map = Maps.newHashMap();
            widgetFolders.forEach(f -> {
                Long parentId = f.getParentId();
                WidgetFolderDto widgetFolderDto = new WidgetFolderDto(f);
                Set<WidgetFolderDto> sets = map.get(parentId);
                if (sets == null) {
                    map.put(parentId, Sets.newHashSet(widgetFolderDto));
                } else {
                    sets.add(widgetFolderDto);
                    map.put(parentId, sets);
                }
            });

            // 递归文件夹,建立树状关系
            buildFolderTree(root, map);
        }
        return root;
    }

    /**
     * 递归建立树状文件夹的关系
     *
     * @param parentFolder 父级文件夹
     * @param map          parentId -> Set<WidgetFolderDto>
     */
    private void buildFolderTree(WidgetFolderDto parentFolder, Map<Long, Set<WidgetFolderDto>> map) {
        Set<WidgetFolderDto> widgetFolders = map.get(parentFolder.getId());
        if (!CollectionUtils.isEmpty(widgetFolders)) {
            parentFolder.setChildren(widgetFolders);
            widgetFolders.forEach(folder -> {
                buildFolderTree(folder, map);
            });
        }
    }
}
