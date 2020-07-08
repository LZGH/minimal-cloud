package com.lzpeng.minimal.common.jpa.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lzpeng.minimal.common.core.annotation.Excel;
import com.lzpeng.minimal.common.jpa.annotation.BooleanValue;
import com.lzpeng.minimal.common.jpa.support.GenerateEntityIdListener;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 多数据源 https://www.jianshu.com/p/9f812e651319
 * 列注释 http://www.majunwei.com/view/201707241152140494.html
 * 注解@JsonFormat 主要是后台到前台的时间格式的转换
 * 注解@DateTimeFormat 主要是前后到后台的时间格式的转换
 * JPA的CascadeType的解释: https://www.jianshu.com/p/ae07c9f147bc
 * DynamicInsert Insert 时不插入 null, 可以使数据库默认值生效
 * 要使用包装类型变量，不要使用基本类型变量
 * 基础的关系型数据库实体
 *
 * @author: Lzpeng
 */
@Data
@DynamicInsert
@DynamicUpdate
@MappedSuperclass
@EnableJpaAuditing
@EntityListeners({AuditingEntityListener.class, GenerateEntityIdListener.class})
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
public class BaseEntity implements Serializable {

    /**
     * 序列化id
     */
    @Transient
    protected static final long serialVersionUID = 1L;

    @Id
    @ApiModelProperty(value = "主键ID", hidden = true)
    @Column(columnDefinition = "varchar(255) COMMENT 'id 主键'", updatable = false)
    private String id;

    @Excel(name = "是否禁用")
    @ApiModelProperty(value = "是否禁用")
    @BooleanValue(trueValue = "启用", falseValue = "禁用")
    @Column(columnDefinition = "bit DEFAULT b'1' COMMENT '是否禁用,默认启用'")
    private Boolean enabled;
    /**
     * 创建时间
     */
    @CreatedDate
    @Excel(name = "创建时间", imported = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(columnDefinition = "datetime COMMENT '创建时间'", updatable = false)
    private Date createTime;
    /**
     * 最后修改时间
     */
    @LastModifiedDate
    @Excel(name = "最后修改时间", imported = false)
    @ApiModelProperty(value = "最后修改时间", hidden = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(columnDefinition = "datetime COMMENT '最后更新时间'")
    private Date updateTime;
    /**
     * 创建者
     */
    @CreatedBy
    @Excel(name = "创建者", imported = false)
    @ApiModelProperty(value = "创建者", hidden = true)
    @Column(columnDefinition = "varchar(255) COMMENT '创建者'", updatable = false)
    private String createBy;
    /**
     * 最后修改者
     */
    @LastModifiedBy
    @Excel(name = "最后修改者", imported = false)
    @ApiModelProperty(value = "最后修改者", hidden = true)
    @Column(columnDefinition = "varchar(255) COMMENT '最后修改者'")
    private String updateBy;

    @Version
    @ApiModelProperty(value = "版本号", hidden = true)
    @Column(columnDefinition = "bigint COMMENT '版本号'")
    private Long version;

    @Excel(name = "备注")
    @ApiModelProperty(value = "备注", hidden = true)
    @Column(columnDefinition = "varchar(255) COMMENT '备注'")
    private String remark;


}
