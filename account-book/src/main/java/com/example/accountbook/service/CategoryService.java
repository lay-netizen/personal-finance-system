package com.example.accountbook.service;

import com.example.accountbook.entity.Category;
import com.example.accountbook.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 手动添加构造函数
    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * 根据类型获取分类
     */
    public List<Category> getCategoriesByType(Category.CategoryType type) {
        return categoryRepository.findByType(type);
    }

    /**
     * 获取所有分类
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * 根据ID获取分类
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在，ID: " + id));
    }

    /**
     * 新增分类
     */
    public Category createCategory(Category category) {
        if (category.getType() == null) {
            throw new RuntimeException("分类类型不能为空");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("分类名称不能为空");
        }
        return categoryRepository.save(category);
    }

    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    /**
     * 初始化默认分类（如果数据库为空）
     */
    public void initializeDefaultCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = Arrays.asList(
                    // 收入分类
                    createCategory("工资", Category.CategoryType.INCOME, "💰", "#4CAF50"),
                    createCategory("奖金", Category.CategoryType.INCOME, "🎁", "#4CAF50"),
                    createCategory("投资回报", Category.CategoryType.INCOME, "📈", "#4CAF50"),
                    createCategory("其他收入", Category.CategoryType.INCOME, "💸", "#4CAF50"),

                    // 支出分类
                    createCategory("餐饮", Category.CategoryType.EXPENSE, "🍔", "#FF9800"),
                    createCategory("交通", Category.CategoryType.EXPENSE, "🚗", "#2196F3"),
                    createCategory("购物", Category.CategoryType.EXPENSE, "🛍️", "#E91E63"),
                    createCategory("娱乐", Category.CategoryType.EXPENSE, "🎬", "#9C27B0"),
                    createCategory("医疗", Category.CategoryType.EXPENSE, "🏥", "#F44336"),
                    createCategory("教育", Category.CategoryType.EXPENSE, "📚", "#3F51B5"),
                    createCategory("房租", Category.CategoryType.EXPENSE, "🏠", "#795548"),
                    createCategory("水电煤", Category.CategoryType.EXPENSE, "💡", "#FFC107"),
                    createCategory("其他支出", Category.CategoryType.EXPENSE, "📦", "#607D8B")
            );
            categoryRepository.saveAll(defaultCategories);
        }
    }

    private Category createCategory(String name, Category.CategoryType type, String icon, String color) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        category.setColor(color);
        return category;
    }
}