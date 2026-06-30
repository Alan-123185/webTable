package com.xushu.webtable.Tasks;

import com.xushu.webtable.common.Const;
import com.xushu.webtable.common.File;
import com.xushu.webtable.mapper.recycleBinMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class RecycleCleanupTask {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private recycleBinMapper recycleBinMapper;

    /**
     * 定时任务，每天0点执行一次
     * 清理回收站
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24)
    public void cleanup() {
        List<Integer> rubishids = recycleBinMapper.getrubish();
        for (Integer id : rubishids) {
            File f = recycleBinMapper.selectFileInBin(id);
            if (f == null) continue;
            Object count = redisTemplate.opsForValue().get(Const.REDIS_DOWNLOAD_LOCK_KEY + f.getId());
            if(count!=null && (Integer)count >0)
                continue;
            if (recycleBinMapper.binfilecount(f.getFileName()) == 1) {
                try {
                    // 再次确认状态（防止加锁期间变化）
                    Files.delete(Path.of(f.getPath()));
                    recycleBinMapper.deletebinfile(id); // 数据库删除
                } catch (Exception e) {
                    log.error("物理文件删除失败（将稍后重试或人工处理）: {}", f.getPath(), e);
                }
            }
        }
    }
}



