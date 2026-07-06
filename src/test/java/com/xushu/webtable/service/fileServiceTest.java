package com.xushu.webtable.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.pagehelper.Page;
import com.xushu.webtable.ManageException;
import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.File;
import com.xushu.webtable.common.User;
import com.xushu.webtable.common.selectFileBean;
import com.xushu.webtable.common.shareinfo;
import com.xushu.webtable.mapper.fileMapper;
import com.xushu.webtable.mapper.uploadMapper;
import com.xushu.webtable.service.fileService;
import com.xushu.webtable.service.maker.fileServicemake;
import com.xushu.webtable.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "user.volume=1073741824",
        "extName.type=jpg,png,gif,mp4",
        "path.pathhead=/data/files/"
})
class fileServiceTest {
    @Autowired
    private MockMvc mockMvc;

    @Mock
    private fileMapper fileMapper;

    @Mock
    private uploadMapper uploadMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private fileServicemake fileService;

    @Mock
    private recycleBinService recycleBinService;

    // 集成测试使用的真实 Bean
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private fileService fileServiceReal;

    /**
     * 初始化测试环境，设置 fileService 中的配置字段
     */
    @BeforeEach
    void setUp() {
        // ========== 集成测试数据库准备 ==========
        // 建表（幂等，只在首次执行）
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `user` (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, user_name VARCHAR(255), password VARCHAR(255), " +
                "volume BIGINT, email VARCHAR(255), all_volume BIGINT, role INT, status INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `file` (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, file_name VARCHAR(255), original_file_name VARCHAR(255), " +
                "user_id INT, file_size BIGINT, path VARCHAR(500), create_time DATETIME, " +
                "ref_count INT, is_folder INT, parent_id INT)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `recycle_bin` (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, file_id INT, user_id INT, create_time DATETIME, " +
                "file_size BIGINT, file_name VARCHAR(255), recycle_time DATETIME, path VARCHAR(500), " +
                "is_folder INT, parent_id INT, original_file_name VARCHAR(255))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `file_link` (" +
                "file_id INT, link_name VARCHAR(255), lose_time DATETIME)");

        // 清理所有测试数据（保证每次测试独立）
        jdbcTemplate.update("DELETE FROM file_link");
        jdbcTemplate.update("DELETE FROM recycle_bin");
        jdbcTemplate.update("DELETE FROM file");
        jdbcTemplate.update("DELETE FROM `user`");

        // 插入默认测试用户
        jdbcTemplate.update("INSERT INTO `user` (id, user_name, password, volume, email, all_volume, role, status) " +
                "VALUES (100, 'testuser', 'Test12345', 1073741824, 'test@test.com', 1073741824, 1, 0)");

        // ========== Mock 测试配置 ==========
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
        u.setId(1);
        // 使用 Page 对象，实现中会将 List<File> 强转为 Page<File>
        Page<File> page = new Page<>();
        page.add(f);
        page.setTotal(1);
        when(fileMapper.selectFile("fileName", 1, 0))
                .thenReturn(page);
        selectFileBean files = fileService.select("fileName", 1, 0, 10, 0);
        assertNotNull(files);
        assertEquals(1, files.getFiles().size());
        assertEquals("fileName", files.getFiles().get(0).getFileName());
        verify(fileMapper).selectFile("fileName", 1, 0);
    }

    // ======================== softdelete 集成测试 ========================

    /**
     * 场景1: 删除单个文件（last reference, ref_count=1）<br>
     * 文件本身是唯一引用 → 移入回收站、清空所有者、删除 file 记录、扣减用户空间
     */
    @Test
    void delete_singleFile_lastReference() {
        // given — 一个文件（它同时也是原始文件，即 ref_count=1）
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (1, 'md5hash1', 'test.txt', 100, 1024, '/data/files/test.txt', NOW(), 1, 0, 0)");

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            // when
            fileServiceReal.softdelete(List.of(1));

            // then-1: recycle_bin 中应有一条记录
            Integer binCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM recycle_bin WHERE file_id = 1", Integer.class);
            assertEquals(1, binCount, "回收站应该有 1 条记录");

            // then-2: file 表中记录已删除
            Integer fileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file WHERE id = 1", Integer.class);
            assertEquals(0, fileCount, "file 表记录应被删除");

            // then-3: 用户空间减少 1024
            Long volume = jdbcTemplate.queryForObject(
                    "SELECT volume FROM `user` WHERE id = 100", Long.class);
            assertEquals(1073741824L - 1024L, volume, "用户空间应减少文件大小");
        }
    }

    /**
     * 场景2: 删除共享文件的副本（ref_count>1，仅删除逻辑文件）<br>
     * 原始文件有其他用户引用 → 只删除当前用户的逻辑记录，ref_count 减一
     */
    @Test
    void delete_sharedFile_copy() {
        // given — 原始文件（由用户 200 拥有，ref_count=2）
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (1, 'md5hash1', 'original.txt', 200, 2048, '/data/files/original.txt', NOW(), 2, 0, 0)");
        // 当前用户的逻辑副本（ref_count=0）
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (2, 'md5hash1', 'copy.txt', 100, 2048, '/data/files/copy.txt', NOW(), 0, 0, 0)");

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            // when
            fileServiceReal.softdelete(List.of(2));

            // then-1: recycle_bin 有逻辑文件的记录
            Integer binCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM recycle_bin WHERE file_id = 2", Integer.class);
            assertEquals(1, binCount, "回收站应有逻辑文件的记录");

            // then-2: 逻辑文件 (id=2) 从 file 表删除
            Integer logicalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file WHERE id = 2", Integer.class);
            assertEquals(0, logicalCount, "逻辑文件记录应被删除");

            // then-3: 原始文件 (id=1) 仍在，且 ref_count=1
            Integer refCount = jdbcTemplate.queryForObject(
                    "SELECT ref_count FROM file WHERE id = 1", Integer.class);
            assertEquals(1, refCount, "原始文件 ref_count 应减 1");

            // then-4: 用户空间减少
            Long volume = jdbcTemplate.queryForObject(
                    "SELECT volume FROM user WHERE id = 100", Long.class);
            assertEquals(1073741824L - 2048L, volume, "用户空间应减少");
        }
    }

    /**
     * 场景3: 权限不足 — 删除别人拥有的文件 → 抛出 ManageException<br>
     * 验证没有任何数据被修改
     */
    @Test
    void delete_permissionDenied() {
        // given — 属于用户 200 的文件
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (1, 'md5hash1', 'others.txt', 200, 1024, '/data/files/others.txt', NOW(), 1, 0, 0)");

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100); // 当前用户是 100

            // when & then — 抛出权限错误
            assertThrows(ManageException.class, () -> fileServiceReal.softdelete(List.of(1)),
                    "应抛出权限错误");

            // 验证数据未被修改
            Integer fileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file WHERE id = 1", Integer.class);
            assertEquals(1, fileCount, "文件记录应完整保留");
            Integer binCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM recycle_bin", Integer.class);
            assertEquals(0, binCount, "回收站应为空");
        }
    }

    /**
     * 场景4: 批量删除多个文件<br>
     * 同时删除两个独立文件，验证批量操作
     */
    @Test
    void delete_multipleFiles() {
        // given — 两个独立文件
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (1, 'md5hash_a', 'a.txt', 100, 512, '/data/files/a.txt', NOW(), 1, 0, 0)");
        jdbcTemplate.update("INSERT INTO file (id, file_name, original_file_name, user_id, file_size, path, " +
                "create_time, ref_count, is_folder, parent_id) " +
                "VALUES (2, 'md5hash_b', 'b.txt', 100, 1024, '/data/files/b.txt', NOW(), 1, 0, 0)");

        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            // when
            fileServiceReal.softdelete(List.of(1, 2));

            // then-1: 两个文件都已从 file 表删除
            Integer fileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM file WHERE id IN (1, 2)", Integer.class);
            assertEquals(0, fileCount, "文件记录应被删除");

            // then-2: 回收站有两条记录
            Integer binCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM recycle_bin WHERE file_id IN (1, 2)", Integer.class);
            assertEquals(2, binCount, "回收站应有 2 条记录");

            // then-3: 用户空间减少两个文件大小之和
            Long volume = jdbcTemplate.queryForObject(
                    "SELECT volume FROM user WHERE id = 100", Long.class);
            assertEquals(1073741824L - (512L + 1024L), volume, "用户空间应减少");
        }
    }

    /**
     * 场景5: 删除不存在的文件 — 验证 NPE（生产代码未做 null 检查）
     */
    @Test
    void delete_notExist() {
        try (MockedStatic<CurrentHolder> holder = mockStatic(CurrentHolder.class)) {
            holder.when(CurrentHolder::get).thenReturn(100);

            // selectFileById(999) 返回 null，getIsFolder() 抛 NPE
            assertThrows(NullPointerException.class, () -> fileServiceReal.softdelete(List.of(999)));
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
        String newName = "renamed";

        File mockFile = new File();
        mockFile.setOriginalFileName("oldname.txt");  // 必须有原始文件名

        when(fileMapper.selectFileById(fileId)).thenReturn(mockFile);
        when(fileMapper.rename(fileId, newName+".txt")).thenReturn(1);

        Integer result = fileService.rename(fileId, newName);

        assertEquals(1, result);
        verify(fileMapper).rename(fileId, newName+".txt");
    }
}
