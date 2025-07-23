package com.example.demo.mapper;

import com.example.demo.entity.TestEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestEntityMapper {
    @Select("SELECT id, name FROM test_entity WHERE id = #{id}")
    TestEntity findById(Long id);

    @Insert("INSERT INTO test_entity (name) VALUES (#{name})")
    void insert(TestEntity entity);
} 