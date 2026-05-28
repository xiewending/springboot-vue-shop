package com.shoppilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoppilot.common.BusinessException;
import com.shoppilot.dto.LoginRequest;
import com.shoppilot.entity.SysMenu;
import com.shoppilot.entity.SysRole;
import com.shoppilot.entity.SysRoleMenu;
import com.shoppilot.entity.SysUser;
import com.shoppilot.entity.SysUserRole;
import com.shoppilot.mapper.SysMenuMapper;
import com.shoppilot.mapper.SysRoleMapper;
import com.shoppilot.mapper.SysRoleMenuMapper;
import com.shoppilot.mapper.SysUserMapper;
import com.shoppilot.mapper.SysUserRoleMapper;
import com.shoppilot.security.JwtTokenProvider;
import com.shoppilot.security.PasswordHasher;
import com.shoppilot.vo.LoginResponse;
import com.shoppilot.vo.MenuVO;
import com.shoppilot.vo.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final String MENU_TYPE_BUTTON = "B";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysMenuMapper sysMenuMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRoleMenuMapper sysRoleMenuMapper,
            PasswordHasher passwordHasher,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));

        if (user == null || !passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "用户已被禁用");
        }

        RbacPayload rbacPayload = loadRbacPayload(user.getId());

        return new LoginResponse(
                jwtTokenProvider.createToken(user),
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                new UserInfo(user.getId(), user.getUsername(), user.getNickname()),
                rbacPayload.roles(),
                rbacPayload.permissions(),
                rbacPayload.menus()
        );
    }

    private RbacPayload loadRbacPayload(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return new RbacPayload(List.of(), List.of(), List.of());
        }

        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds)
                .stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .sorted(Comparator.comparing(SysRole::getId))
                .toList();
        List<Long> enabledRoleIds = roles.stream().map(SysRole::getId).toList();
        if (enabledRoleIds.isEmpty()) {
            return new RbacPayload(List.of(), List.of(), List.of());
        }

        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, enabledRoleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            return new RbacPayload(
                    roles.stream().map(SysRole::getRoleCode).toList(),
                    List.of(),
                    List.of()
            );
        }

        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds)
                .stream()
                .filter(menu -> menu.getStatus() != null && menu.getStatus() == 1)
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .toList();

        List<String> permissions = menus.stream()
                .map(SysMenu::getPermissionCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Set<Long> accessibleMenuIds = menus.stream().map(SysMenu::getId).collect(Collectors.toSet());
        List<MenuVO> visibleMenus = menus.stream()
                .filter(menu -> !MENU_TYPE_BUTTON.equals(menu.getMenuType()))
                .filter(menu -> menu.getVisible() != null && menu.getVisible() == 1)
                .filter(menu -> menu.getParentId() == 0 || accessibleMenuIds.contains(menu.getParentId()))
                .map(this::toMenuVO)
                .toList();

        return new RbacPayload(
                roles.stream().map(SysRole::getRoleCode).toList(),
                permissions,
                buildMenuTree(visibleMenus)
        );
    }

    private List<MenuVO> buildMenuTree(List<MenuVO> menus) {
        Map<Long, MenuVO> menuMap = menus.stream()
                .collect(Collectors.toMap(MenuVO::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0 || !menuMap.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                menuMap.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        return roots;
    }

    private MenuVO toMenuVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getMenuName());
        vo.setPath(menu.getRoutePath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setPermission(menu.getPermissionCode());
        return vo;
    }

    private record RbacPayload(List<String> roles, List<String> permissions, List<MenuVO> menus) {
    }
}
