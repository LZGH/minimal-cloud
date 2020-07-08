package com.lzpeng.minimal.common.jpa.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.TypeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lzpeng.minimal.common.core.response.QueryResult;
import com.lzpeng.minimal.common.core.util.BeanUtils;
import com.lzpeng.minimal.common.core.util.ExcelUtils;
import com.lzpeng.minimal.common.jpa.domain.dto.BatchModel;
import com.lzpeng.minimal.common.jpa.domain.entity.BaseEntity;
import com.lzpeng.minimal.common.jpa.repository.BaseRepository;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 单表Service
 * @author: Lzpeng
 */
@Transactional(rollbackOn = Exception.class)
public abstract class BaseService<Entity extends BaseEntity> {

    /**
     * 默认路径
     */
    protected static final String USER_DIR = System.getProperty("user.dir");

    /**
     * 临时文件路径
     */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Autowired
    protected ObjectMapper objectMapper;
    /**
     * 不能使用@Autowired 有 bug 不能泛型注入
     */
    protected BaseRepository<Entity> baseRepository;

    /**
     * 查询条件
     * 模糊匹配
     * 忽略空值
     * 忽略大小写
     * 忽略字段，即不管password是什么值都不加入查询条件
     */
    private ExampleMatcher matcher = ExampleMatcher.matching()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
            .withIgnoreNullValues()
            .withIgnoreCase()
            .withIgnorePaths("password");

    /**
     * 保存实体
     * @param entity 要保存的实体
     * @return 保存成功的实体
     */
    public Entity save(Entity entity) {
        if (beforeSave(entity)) {
            entity = baseRepository.save(entity);
            return entity;
        } else {
            throw new RuntimeException("保存失败");
        }
    }

    /**
     * 保存实体列表
     * @param entities 实体列表
     * @return 保存成功的实体列表
     */
    public List<Entity> saveAll(Iterable<Entity> entities) {
        Assert.notNull(entities, "Entities must not be null!");
        List<Entity> result = new ArrayList<Entity>();
        for (Entity entity : entities) {
            result.add(save(entity));
        }
        return result;
    }


    /**
     * 根据 id 删除实体
     * @param id 实体 id
     */
    public void delete(String id) {
        baseRepository.deleteById(id);
    }

    /**
     * 删除所有实体
     */
    public void deleteAll() {
        baseRepository.deleteAll();
    }

    /**
     * 根据 id 更新实体
     * @param id id
     * @param model 更新的实体
     * @return 更新后的结果
     */
    public Entity update(String id, Entity model) {
        Optional<Entity> optional = baseRepository.findById(id);
        if (optional.isPresent()) {
            Entity entity = optional.get();
            BeanUtils.convertEmptyCollectionToNull(model);
            BeanUtil.copyProperties(model, entity, CopyOptions.create().setIgnoreNullValue(true));
            return save(entity);
        }
        return null;
    }

    /**
     * 根据分页条件查询实体
     * @param page 页码
     * @param size 每页行数
     * @return 符合条件的实体列表
     */
    public QueryResult<Entity> query(int page, int size) {
        return query(page, size, (Entity)null);
    }

    /**
     * 根据查询条件和分页条件查询实体
     * @param page 页码
     * @param size 每页行数
     * @param model JPA Example 查询条件
     * @return 符合条件的实体列表
     */
    public QueryResult<Entity> query(int page, int size, Entity model) {
        // 处理不正确的页码
        page = optimizePage(page);
        // 处理不正确的每页数据量
        size = optimizeSize(size);
        // 得到分页对象
        Pageable pageable = getPageable(page, size);
        Page<Entity> pageResult;
        if (model == null) {
            // 没有传查询条件
            pageResult = baseRepository.findAll(pageable);
        } else {
            // 将空白字符 或 undefined 设置为 null
            BeanUtils.convertBlankToNull(model);
            pageResult = baseRepository.findAll(Example.of(model, matcher), pageable);
        }
        // 执行查询后操作
        return new QueryResult(pageResult.getContent(), pageResult.getTotalElements(), pageResult.getNumber() + 1, pageResult.getTotalPages());
    }

    /**
     * 根据查询条件和分页条件查询实体
     * @param page 页码
     * @param size 每页行数
     * @param predicate Query DSL查询条件
     * @return 符合条件的实体列表
     */
    public QueryResult<Entity> query(int page, int size, Predicate predicate) {
        // 处理不正确的页码
        page = optimizePage(page);
        // 处理不正确的每页数据量
        size = optimizeSize(size);
        // 得到分页对象
        Pageable pageable = getPageable(page, size);
        Page<Entity> pageResult;
        if (predicate == null) {
            // 没有传查询条件
            pageResult = baseRepository.findAll(pageable);
        } else {
            pageResult = baseRepository.findAll(predicate, pageable);
        }
        // 执行查询后操作
        return new QueryResult(pageResult.getContent(), pageResult.getTotalElements(), pageResult.getNumber() + 1, pageResult.getTotalPages());
    }

    /**
     * 根据复杂查询条件查询实体,若查询到多个抛异常
     * @return 符合条件的实体
     */
    public Entity findOne(Specification<Entity> specification) {
        Optional<Entity> optional = baseRepository.findOne(specification);
        return optional.orElse(null);
    }

    /**
     * 根据查询条件查询实体,若查询到多个抛异常
     * @return 符合条件的实体
     */
    public Entity findOne(Entity model) {
        Optional<Entity> optional = baseRepository.findOne(Example.of(model));
        return optional.orElse(null);
    }

    /**
     * 根据复杂查询条件查询实体
     * @return 符合条件的实体列表
     */
    public List<Entity> findAll(Specification<Entity> specification) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        return baseRepository.findAll(specification, sort);
    }

    /**
     * 根据查询条件查询实体
     * @param predicate query dsl 查询条件
     * @return 符合条件的实体列表
     */
    public List<Entity> findAll(Predicate predicate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Iterable<Entity> iterable = baseRepository.findAll(predicate, sort);
        return ListUtil.toList(iterable);
    }
    /**
     * 根据查询条件和排序条件查询实体
     * @param predicate query dsl 查询条件
     * @param sort jpa 排序条件
     * @return 符合条件的实体列表
     */
    public List<Entity> findAll(Predicate predicate, Sort sort) {
        sort = getSortAppendCreateTime(sort);
        Iterable<Entity> iterable = baseRepository.findAll(predicate, sort);
        return ListUtil.toList(iterable);
    }

    /**
     * 查询所有实体
     * @return 所有实体列表
     */
    public List<Entity> findAll() {
        return findAll((Entity) null);
    }
    /**
     * 根据排序条件查询所有实体
     * @param sort jpa 排序条件
     * @return 排序后的实体列表
     */
    public List<Entity> findAll(Sort sort) {
        return findAll((Entity) null, sort);
    }

    /**
     * 根据查询条件查询实体
     * @param model 模糊查询条件
     * @return 符合条件的实体列表
     */
    public List<Entity> findAll(Entity model) {
        return findAll(model, null);
    }
    /**
     * 根据查询条件和排序条件查询实体
     * @param model 模糊查询条件
     * @param sort 排序条件
     * @return 符合条件的实体列表
     */
    public List<Entity> findAll(Entity model, Sort sort) {
        sort = getSortAppendCreateTime(sort);
        if (model == null) {
            // 没有传查询条件
            List<Entity> entities = baseRepository.findAll(sort);
            // afterFindAll(entities);
            return entities;
        } else {
            // 将空白字符 或 undefined 设置为 null
            BeanUtils.convertBlankToNull(model);
            List<Entity> entities = baseRepository.findAll(Example.of(model, matcher), sort);
            // afterFindAll(entities);
            return entities;
        }
    }

    /**
     * 根据id查询实体
     * @param id id
     * @return 查询到的实体
     */
    public Entity findById(String id) {
        Optional<Entity> optional = baseRepository.findById(id);
        if (optional.isPresent()) {
            Entity entity = optional.get();
            return entity;
        }
        return null;
    }


    /**
     * 从 json 读取实体
     * @param json JSON 字符串
     * @return 读取成功的实体列表
     * @throws JsonProcessingException JSON异常
     */
    public List<Entity> readDataFromJson(String json) throws JsonProcessingException {
        List<Entity> list = objectMapper.readValue(json, new TypeReference<List<Entity>>() {
        });
        return list;
    }

    /**
     * 从 Excel文件输入流读取实体
     * @param inputStream Excel文件输入流
     * @return 读取成功的实体列表
     */
    public List<Entity> readDataFromExcel(InputStream inputStream) {
        List<Entity> list = ExcelUtils.readDataFromStream(inputStream, getEntityClass());
        return list;
    }


    /**
     * 从json导入实体
     * @param json json 字符串
     * @return 导入成功的实体列表
     * @throws JsonProcessingException JSON 解析异常
     */
    public List<Entity> importDataFromJson(String json) throws JsonProcessingException {
        List<Entity> collection = readDataFromJson(json);
        return saveAll(collection);
    }


    /**
     * 从 Excel文件输入流导入到数据库
     * @param inputStream Excel文件
     * @return 导入成功的实体列表
     */
    public List<Entity> importDataFromExcel(InputStream inputStream) {
        List<Entity> collection = readDataFromExcel(inputStream);
        return saveAll(collection);
    }
    /**
     * 从文件导入实体
     * @param file 上传的文件
     * @return 导入成功的实体列表
     */
    public List<Entity> importData(MultipartFile file) throws IOException {
        List<Entity> list = null;
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename.toLowerCase());
        switch (extName) {
            case "json":
                list = importDataFromJson(IoUtil.read(file.getInputStream(), Charset.defaultCharset()));
                break;
            case "xls":
            case "xlsx":
                list = importDataFromExcel(file.getInputStream());
                break;
            case "xml":
                // TODO 导入xml
                break;
            default:
                throw new RuntimeException("不支持的文件类型: " + extName);
        }
        return list;
    }

    /**
     * 将在 ids 列表中的实体导出到 Excel 并写到HTTP响应
     * @param ids 单据 Id 列表
     * @param response 响应
     */
    public void exportData(List<String> ids, HttpServletResponse response) throws IOException {
        InputStream inputStream = exportDataToExcel(ids);
        String fileName = getEntityClass().getSimpleName() + System.currentTimeMillis() + ".xlsx";
        //fileInfoService.downloadFile(fileName, inputStream, response);
    }

    /**
     * 将在 ids 列表中的实体导出到 Excel
     * @param ids 单据 Id 列表
     * @return excel 文件输入流
     */
    public InputStream exportDataToExcel(List<String> ids) throws IOException {
        List<Entity> list = CollectionUtils.isEmpty(ids) ? findAll() : findAllById(ids);
        Class<Entity> clazz = getEntityClass();
        String tempDir = getTempDir();
        Path path = Paths.get(tempDir, clazz.getSimpleName() + ".xlsx");
        ExcelUtils.writeDataToFile(list, path.toFile());
        InputStream inputStream = Files.newInputStream(path);
        FileUtil.del(tempDir);
        return inputStream;
    }


    /**
     * 批量增删改查
     * @param batch 批量操作的数据
     * @return 操作结果
     */
    public Object batch(BatchModel<Entity> batch) {
        if (batch.getDelete() != null) {
            List<Entity> entities = baseRepository.findAllById(Arrays.asList(batch.getDelete()));
            baseRepository.deleteInBatch(entities);
            return "批量删除成功";
        }
        if (batch.getUpdate() != null) {
            for (Map.Entry<String, Entity> entry : batch.getUpdate().entrySet()) {
                update(entry.getKey(), entry.getValue());
            }
            return "批量修改成功";
        }
        if (batch.getCreate() != null) {
            saveAll(Arrays.asList(batch.getCreate()));
            return "批量修改成功";
        }
        return null;
    }

    /**
     * 得到在 ids 列表中的实体
     * select * from table_name where id  in (ids)
     * @param ids id 列表
     * @return 查询在 ids 中的实体
     */
    public List<Entity> findAllById(Iterable<String> ids) {
        return baseRepository.findAllById(ids);
    }

    /**
     * 得到不在 ids 列表中的实体
     * select * from table_name where id not in (ids)
     * @param ids id 列表
     * @return 查询不在 ids 中的实体
     */
    public List<Entity> findAllByIdNotIn(Iterable<String> ids) {
        return baseRepository.findAllByIdNotIn(ids);
    }


    /**
     * 根据 id 启用实体
     * @param id 待启用的实体 id
     * @return 启用成功的行数
     */
    public int enabled(String id) {
        return baseRepository.updateEnabled(id, true);
    }

    /**
     * 根据 id 禁用实体
     * @param id 待禁用的实体 id
     * @return 禁用成功的行数
     */
    public int disabled(String id) {
        return baseRepository.updateEnabled(id, false);
    }

    /**
     * 查询总行数
     * @return 查询总行数
     */
    public long count() {
        return baseRepository.count();
    }

    /**
     * 根据 model 查询符合条件的行数
     * @param model 查询条件
     * @return 符合条件的行数
     */
    public long count(Entity model) {
        if (model == null) {
            // 没有传查询条件
            return count();
        } else {
            // 将空白字符 或 undefined 设置为 null
            BeanUtils.convertBlankToNull(model);
            return baseRepository.count(Example.of(model, matcher));
        }
    }

    /**
     * 根据 predicate 查询符合条件的行数
     * @param predicate query DSL 查询条件
     * @return 符合条件的行数
     */
    public long count(Predicate predicate) {
        return baseRepository.count(predicate);
    }


    /**
     * 得到泛型参数 -- 实体类型
     * @return 实体类型
     */
    protected Class<Entity> getEntityClass() {
        Type type = TypeUtil.getTypeArgument(getClass());
        if (type != null && type instanceof Class) {
            return (Class<Entity>) type;
        }
        return null;
    }

    /**
     * 得到临时文件夹
     * @return 临时文件夹
     */
    protected String getTempDir() {
        return Paths.get(USER_DIR, String.valueOf(System.nanoTime())).toString();
    }


    /**
     * 保存前操作
     * @param entity 要保存的实体
     * @return 是否还需要保存这个实体
     */
    protected boolean beforeSave(Entity entity) {
        return true;
    }

    /**
     * 保存前操作
     * @param entities 要保存的实体集合
     * @return 是否还需要保存这个实体集合
     */
    protected boolean beforeSaveAll(Collection<Entity> entities) {
        for (Entity entity : entities) {
            if (!beforeSave(entity)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 得到JPA分页对象
     * @param page 第几页
     * @param size 每页几条数据
     * @return JPA分页对象
     */
    private Pageable getPageable(int page, int size) {
        //Pageable 和 Page 接口介绍: https://blog.csdn.net/u011781521/article/details/74539330
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        return PageRequest.of(page, size, sort);
    }

    /**
     * 若页码小于等于0 则返回20
     * @param size 前端传来的页码
     * @return 经过优化的页码
     */
    private int optimizeSize(int size) {
        if (size <= 0) {
            // 如果传入size 不合法则设置为 20
            size = 20;
        }
        return size;
    }

    /**
     * 若页码小于等于0,则返回第一页的页码
     * 将页码减 1 以适应JPA分页查询
     * @param page 前端传来的页码
     * @return 经过优化的页码
     */
    private int optimizePage(int page) {
        if (page <= 0) {
            page = 1;
        }
        //为了适应数据库,将页码减1
        page = page - 1;
        return page;
    }

    /**
     * 排序条件增加按时间降序
     * @param sort 原排序条件
     * @return 加入创建时间倒序
     */
    private Sort getSortAppendCreateTime(Sort sort) {
        if (sort == null) {
            sort = Sort.by(Sort.Direction.DESC, "createTime");
        } else {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "createTime"));
        }
        return sort;
    }

}
