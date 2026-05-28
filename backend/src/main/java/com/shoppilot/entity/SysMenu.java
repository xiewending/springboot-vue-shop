package com.shoppilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String routePath;

    private String component;

    private String icon;

    private String permissionCode;

    private Integer sortOrder;

    private Integer visible;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
