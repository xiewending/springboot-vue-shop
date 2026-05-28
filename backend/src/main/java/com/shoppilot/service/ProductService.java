package com.shoppilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoppilot.common.BusinessException;
import com.shoppilot.dto.ProductQueryRequest;
import com.shoppilot.dto.ProductSaveRequest;
import com.shoppilot.entity.Category;
import com.shoppilot.entity.Product;
import com.shoppilot.mapper.CategoryMapper;
import com.shoppilot.mapper.ProductMapper;
import com.shoppilot.vo.PageResult;
import com.shoppilot.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public ProductService(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    public PageResult<ProductVO> page(ProductQueryRequest request) {
        long requestedPage = request.getPage() == null ? 1L : request.getPage();
        long requestedSize = request.getSize() == null ? 10L : request.getSize();
        long page = Math.max(requestedPage, 1L);
        long size = Math.min(Math.max(requestedSize, 1L), 100L);

        Long total = productMapper.selectCount(buildQueryWrapper(request));
        var records = productMapper.selectList(buildQueryWrapper(request)
                .last("limit " + ((page - 1) * size) + ", " + size));

        Map<Long, Category> categories = records.isEmpty()
                ? Map.of()
                : categoryMapper.selectBatchIds(records.stream().map(Product::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return new PageResult<>(
                records.stream().map(product -> toVO(product, categories)).toList(),
                total,
                page,
                size
        );
    }

    public ProductVO getById(Long id) {
        Product product = requireProduct(id);
        Category category = categoryMapper.selectById(product.getCategoryId());
        return toVO(product, category == null ? Map.of() : Map.of(product.getCategoryId(), category));
    }

    public Long create(ProductSaveRequest request) {
        ensureCategoryExists(request.getCategoryId());
        Product product = new Product();
        fillProduct(product, request);
        productMapper.insert(product);
        return product.getId();
    }

    public void update(Long id, ProductSaveRequest request) {
        ensureCategoryExists(request.getCategoryId());
        Product product = requireProduct(id);
        fillProduct(product, request);
        productMapper.updateById(product);
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "商品状态只能是 0 或 1");
        }
        Product product = requireProduct(id);
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    private LambdaQueryWrapper<Product> buildQueryWrapper(ProductQueryRequest request) {
        return new LambdaQueryWrapper<Product>()
                .like(StringUtils.hasText(request.getKeyword()), Product::getName, request.getKeyword())
                .eq(request.getCategoryId() != null, Product::getCategoryId, request.getCategoryId())
                .eq(request.getStatus() != null, Product::getStatus, request.getStatus())
                .orderByDesc(Product::getUpdatedAt)
                .orderByDesc(Product::getId);
    }

    private void fillProduct(Product product, ProductSaveRequest request) {
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus());
        product.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    private void ensureCategoryExists(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(400, "商品分类无效");
        }
    }

    private ProductVO toVO(Product product, Map<Long, Category> categories) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        Category category = categories.get(product.getCategoryId());
        vo.setCategoryName(category == null ? "-" : category.getName());
        return vo;
    }
}
