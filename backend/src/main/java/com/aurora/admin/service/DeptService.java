package com.aurora.admin.service;

import com.aurora.admin.entity.Dept;
import java.util.List;

public interface DeptService {
    List<Dept> findAll();
    List<Dept> findTree();
    Dept findById(Long id);
    Dept create(Dept dept);
    Dept update(Long id, Dept dept);
    void delete(Long id);
}
