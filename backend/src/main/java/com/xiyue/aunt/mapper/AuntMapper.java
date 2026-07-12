package com.xiyue.aunt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiyue.aunt.entity.Aunt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 阿姨资料 Mapper。
 */
@Mapper
public interface AuntMapper extends BaseMapper<Aunt> {

    /**
     * 原子更新阿姨评分与服务次数（加权平均）。
     *
     * <p>公式：newRating = ROUND((oldRating * oldCount + #{newRating}) / (oldCount + 1), 1)
     * 单条 SQL 原子完成，避免读-算-写的并发丢失更新问题（ADR-020 优化）。
     *
     * @param auntId    阿姨 ID
     * @param newRating 新评分（1-5）
     * @return 更新行数（1=成功，0=阿姨不存在或已删除）
     */
    @Update("UPDATE aunt SET rating = ROUND((rating * service_count + #{newRating}) / (service_count + 1), 1), " +
            "service_count = service_count + 1, update_time = NOW() " +
            "WHERE id = #{auntId} AND deleted = 0")
    int updateRatingAndCount(@Param("auntId") Long auntId, @Param("newRating") Integer newRating);
}
