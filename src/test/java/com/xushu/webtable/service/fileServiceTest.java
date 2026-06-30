package com.xushu.webtable.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.pagehelper.Page;
import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import com.xushu.webtable.mapper.fileMapper;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.service.maker.fileServicemake;
import com.xushu.webtable.utils.CurrentHolder;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class fileServiceTest {
    @Mock
    private fileMapper fileMapper;

    @Mock
    private uploadMapper uploadMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private fileServicemake fileService;

    @InjectMocks
    private recycleBinService recycleBinService;

    /**
     * 初始化测试环境，设置 fileService 中的配置字段
     */
    @BeforeEach
    void setUp() {
        // 设置 @Value 注入的字段
        ReflectionTestUtils.setField(fileService, "volume", 1073741824L);
        ReflectionTestUtils.setField(fileService, "extName", "jpg,png,gif,mp4");
        ReflectionTestUtils.setField(fileService, "path", "/data/files/");
    }

    /**
     * 测试文件查询功能
     */
    @Test
    void select() {
        File f = new File();
        f.setFileName("fileName");
        User u = new User();
        u.setId(1L);
        // 使用 Page 对象，实现中会将 List<File> 强转为 Page<File>
        Page<File> page = new Page<>();
        page.add(f);
        page.setTotal(1);
        when(fileMapper.selectFile("fileName", 1, 0))
                .thenReturn(page);
        selectFileBean files = fileService.select("fileName",1, 0, 10, 0);
        assertNotNull(files);
        assertEquals(1, files.getFiles().size());
        assertEquals("fileName", files.getFiles().get(0).getFileName());
        verify(fileMapper).selectFile("fileName",1,0);
    }

    /**
     * 测试文件删除功能（引用计数大于1且ID不同的情况）
     */
    @Test
    void delete() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);

        File f = new File();
        f.setId(1);
        f.setUserId(100);
        f.setFileName("md5hash");
        f.setIsFolder(0);
        f.setFileSize(1024L);

        File originalFile = new File();
        originalFile.setId(2);       // id0 = 2, 与 f.id=1 不同
        originalFile.setRefCount(2); // refCount > 1

        User user = new User();
        user.setId(100L);
        user.setVolume(5000L);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            when(fileMapper.selectFileById(1)).thenReturn(f);
            when(uploadMapper.selectByMd5("md5hash")).thenReturn(originalFile);
            when(uploadMapper.selectUserById(100)).thenReturn(user);

            recycleBinService.delete(ids);

            // refCount=2 > 1 且 id(1) != id0(2) → 文件加入待删集合
            verify(uploadMapper).updateRefCount2(2);
            // 有待删文件 → 更新用户空间并批量删除
            verify(uploadMapper).selectUserById(100);
            verify(uploadMapper).updateVolume(argThat(u ->
                    u.getVolume().equals(5000L - 1024L)
            ));
            verify(fileMapper).deleteFile(anyList());
        }
    }
    /**
     * 测试文件移动成功
     */
     @Test
     void move_success() {
         Integer fileId = 1;
         File mockFile = new File();
         mockFile.setId(fileId);
         mockFile.setUserId(100);
         mockFile.setIsFolder(0);
         mockFile.setParentId(0);
         mockFile.setFileName("md5hash");
         mockFile.setOriginalFileName("test.txt");
         File folder = new File();
         folder.setId(2);
         folder.setUserId(100);
         folder.setIsFolder(1);
         folder.setParentId(0);
         when(fileMapper.selectFileById(2)).thenReturn(folder);
         when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
         try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
             holder.when(CurrentHolder::get).thenReturn(100);
             fileService.move(fileId, 2);
             verify(fileMapper).movedir(fileId, 2);
         }
     }

    /**
     * 测试文件移动权限不足，文件夹
     */
    @Test
    void move_permissionDeniedByFolder() {
        Integer fileId = 1;
        File mockFile = new File();
        mockFile.setId(fileId);
        mockFile.setUserId(100);
        mockFile.setIsFolder(0);
        mockFile.setFileName("md5hash");
        mockFile.setOriginalFileName("test.txt");
        File folder = new File();
        folder.setId(2);
        folder.setUserId(101);
        folder.setIsFolder(1);
        folder.setParentId(0);
        when(fileMapper.selectFileById(2)).thenReturn(folder);
        when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
        // 模拟当前用户ID为100
        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);
            assertThrows(ManageException.class, () -> fileService.move(fileId, 2));
        }
    }

    /**
     * 文件不存在移动测试
     */
    @Test
    void move_fileNotExist() {
        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);
            assertThrows(ManageException.class, () -> fileService.move(3, 2));
        }
    }
    /**
     * 文件夹不存在，文件移动测试
     *
     */
    @Test
    void move_folderNotExist() {
        Integer fileId = 1;
        File mockFile = new File();
        mockFile.setId(fileId);
        mockFile.setUserId(100);
        mockFile.setIsFolder(0);
        mockFile.setFileName("md5hash");
        mockFile.setOriginalFileName("test.txt");
        when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);
            assertThrows(ManageException.class, () -> fileService.move(fileId, 2));
        }
    }
    /**
     * 测试文件移动权限不足，文件
     */
    @Test
    void move_permissionDeniedByFile() {
        Integer fileId = 1;
        File mockFile = new File();
        mockFile.setId(fileId);
        mockFile.setUserId(100);
        mockFile.setIsFolder(0);
        mockFile.setFileName("md5hash");
        mockFile.setOriginalFileName("test.txt");
        File folder = new File();
        folder.setId(2);
        folder.setUserId(101);
        folder.setIsFolder(1);
        folder.setParentId(0);
        when(fileMapper.selectFileById(2)).thenReturn(folder);
        when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(200);
            assertThrows(ManageException.class, () -> fileService.move(fileId, 2));
        }
    }


    /**
     * 测试文件分享成功场景
     */
    @Test
    void share_success() {
        Integer fileId = 1;
        File mockFile = new File();
        mockFile.setId(fileId);
        mockFile.setUserId(100);
        mockFile.setIsFolder(0);
        mockFile.setFileName("md5hash");
        mockFile.setOriginalFileName("test.txt");

        File originalFile = new File();
        originalFile.setId(10);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);
            when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
            when(uploadMapper.selectByMd5("md5hash")).thenReturn(originalFile);
            shareinfo result = fileService.share(fileId);
            assertNotNull(result);
            assertNotNull(result.getLinkName());
            assertEquals(8, result.getLinkName().length());
            assertEquals(10, result.getFileId());
            verify(fileMapper).share(eq(10), anyString(), any(LocalDateTime.class));
        }
    }

    /**
     * 测试文件分享权限拒绝场景（非文件所有者）
     */
    @Test
    void share_permissionDenied() {
        Integer fileId = 1;
        File mockFile = new File();
        mockFile.setUserId(200);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);

            assertThrows(ManageException.class, () -> fileService.share(fileId));
        }
    }

    /**
     * 测试文件夹不允许分享场景
     */
    @Test
    void share_folderNotAllowed() {
        Integer fileId = 2;
        File mockFile = new File();
        mockFile.setUserId(100);
        mockFile.setIsFolder(1);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);

            assertThrows(ManageException.class, () -> fileService.share(fileId));
        }
    }

    /**
     * 测试转存链接已过期场景
     */
    @Test
    void store_linkExpired() {
        String link = "expiredlink";
        shareinfo linkInfo = new shareinfo();
        linkInfo.setLoseTime(LocalDateTime.now().minusDays(1));

        when(fileMapper.findlink(link)).thenReturn(linkInfo);

        Integer result = fileService.store(link, 0);
        assertEquals(1, result);
    }

    /**
     * 测试转存链接不存在场景
     */
    @Test
    void store_linkNotFound() {
        String link = "invalidlink";

        when(fileMapper.findlink(link)).thenReturn(null);

        Integer result = fileService.store(link, 0);
        assertEquals(-2, result);
    }

    /**
     * 测试转存成功场景
     */
    @Test
    void store_success() {
        String link = "validlink";
        Integer parentId = 0;

        shareinfo linkInfo = new shareinfo();
        linkInfo.setLoseTime(LocalDateTime.now().plusDays(7));
        linkInfo.setFileId(10);

        File existFile = new File();
        existFile.setId(10);
        existFile.setFileName("md5hash");
        existFile.setOriginalFileName("test.txt");
        existFile.setFileSize(1024L);
        existFile.setPath("/data/files/test.txt");
        existFile.setUserId(200);

        User user = new User();
        user.setVolume(0L);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            when(fileMapper.findlink(link)).thenReturn(linkInfo);
            when(fileMapper.selectFileById(10)).thenReturn(existFile);
            when(uploadMapper.selectUserById(100)).thenReturn(user);

            Integer result = fileService.store(link, parentId);

            assertEquals(0, result);
            verify(uploadMapper).updateRefCount(10);
            verify(uploadMapper).insertRecord(any(File.class));
        }
    }

    /**
     * 测试转存空间不足场景
     */
    @Test
    void store_volumeExceeded() {
        String link = "validlink";

        shareinfo linkInfo = new shareinfo();
        linkInfo.setLoseTime(LocalDateTime.now().plusDays(7));
        linkInfo.setFileId(10);

        File existFile = new File();
        existFile.setId(10);
        existFile.setFileSize(2048L);
        existFile.setFileName("md5hash");
        existFile.setUserId(200);

        User user = new User();
        user.setVolume(1073741824L);

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            when(fileMapper.findlink(link)).thenReturn(linkInfo);
            when(fileMapper.selectFileById(10)).thenReturn(existFile);
            when(uploadMapper.selectUserById(100)).thenReturn(user);

            Integer result = fileService.store(link, 0);
            assertEquals(-3, result);
        }
    }

    /**
     * 测试创建文件夹成功场景
     */
    @Test
    void mkdir_success() {
        String folderName = "newFolder";
        Integer parentId = 0;

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            fileService.mkdir(folderName, parentId);

            verify(uploadMapper).insertRecord(argThat(file ->
                    file.getIsFolder() == 1 &&
                            file.getOriginalFileName().equals(folderName) &&
                            file.getUserId() == 100 &&
                            file.getParentId() == parentId
            ));
        }
    }

    /**
     * 测试获取分享链接信息有效场景
     */
    @Test
    void sharelinkinfo_valid() {
        String link = "validlink";
        shareinfo linkInfo = new shareinfo();
        linkInfo.setLinkName(link);
        linkInfo.setFileId(10);
        linkInfo.setLoseTime(LocalDateTime.now().plusDays(7));

        File mockFile = new File();
        mockFile.setId(10);
        mockFile.setOriginalFileName("shared.txt");

        when(fileMapper.findlink(link)).thenReturn(linkInfo);
        when(fileMapper.selectFileById(10)).thenReturn(mockFile);

        File result = fileService.sharelinkinfo(link);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("shared.txt", result.getOriginalFileName());
    }

    /**
     * 测试获取分享链接信息过期场景
     */
    @Test
    void sharelinkinfo_expired() {
        String link = "expiredlink";
        shareinfo linkInfo = new shareinfo();
        linkInfo.setLoseTime(LocalDateTime.now().minusDays(1));

        when(fileMapper.findlink(link)).thenReturn(linkInfo);

        File result = fileService.sharelinkinfo(link);

        assertNull(result);
    }

    /**
     * 测试获取分享链接信息不存在场景
     */
    @Test
    void sharelinkinfo_notFound() {
        String link = "invalidlink";

        when(fileMapper.findlink(link)).thenReturn(null);

        File result = fileService.sharelinkinfo(link);

        assertNull(result);
    }

    /**
     * 测试重命名文件成功场景
     */
    @Test
    void rename_success() {
        Integer fileId = 1;
        String newName = "renamed.txt";

        when(fileMapper.rename(fileId, newName)).thenReturn(1);

        Integer result = fileService.rename(fileId, newName);

        assertEquals(1, result);
        verify(fileMapper).rename(fileId, newName);
    }
}
