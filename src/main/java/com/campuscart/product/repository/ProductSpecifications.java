package com.campuscart.product.repository;

import com.campuscart.product.domain.MarketplaceScope;
import com.campuscart.product.domain.Product;
import com.campuscart.product.domain.ProductStatus;
import com.campuscart.product.domain.ProductType;
import com.campuscart.product.domain.SellingReach;
import com.campuscart.user.domain.User;
import com.campuscart.user.domain.UserType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> visibleTo(User viewer, MarketplaceScope scope,
                                                    ProductStatus requestedStatus) {
        return (root, query, cb) -> {
            Join<Product, User> seller = root.join("seller");
            Join<Product, com.campuscart.college.domain.College> college = root.join("college", JoinType.LEFT);
            Join<Product, com.campuscart.location.domain.City> city = root.join("city");

            Predicate ownership = cb.equal(seller.get("id"), viewer.getId());
            Predicate statusAllowed = cb.equal(root.get("status"), ProductStatus.ACTIVE);
            if (requestedStatus != null) {
                statusAllowed = cb.and(statusAllowed, cb.equal(root.get("status"), requestedStatus));
            }
            if (viewer.getRole().isAdmin()) {
                return requestedStatus == null ? cb.notEqual(root.get("status"), ProductStatus.DELETED)
                        : cb.equal(root.get("status"), requestedStatus);
            }

            Predicate discovery = discoveryPredicate(viewer, scope, root, seller, college, city, cb);
            Predicate visible = cb.or(ownership, cb.and(statusAllowed, discovery));
            if (requestedStatus != null) {
                visible = cb.and(visible, cb.equal(root.get("status"), requestedStatus));
            }
            return visible;
        };
    }

    private static Predicate discoveryPredicate(User viewer, MarketplaceScope scope,
                                                jakarta.persistence.criteria.Root<Product> root,
                                                Join<Product, User> seller,
                                                Join<Product, com.campuscart.college.domain.College> college,
                                                Join<Product, com.campuscart.location.domain.City> city,
                                                jakarta.persistence.criteria.CriteriaBuilder cb) {
        Predicate publicReach = cb.equal(root.get("sellingReach"), SellingReach.PUBLIC);
        Predicate otherCollegeReach = cb.and(
                cb.equal(root.get("sellingReach"), SellingReach.OTHER_COLLEGES),
                cb.equal(city.get("id"), viewer.getCity().getId()));
        Predicate campusReach = viewer.getCollege() == null
                ? cb.disjunction()
                : cb.and(
                        cb.equal(root.get("sellingReach"), SellingReach.MY_CAMPUS),
                        cb.equal(college.get("id"), viewer.getCollege().getId()));

        return switch (scope) {
            case MY_COLLEGE -> viewer.getCollege() == null
                    ? cb.disjunction()
                    : cb.and(cb.equal(college.get("id"), viewer.getCollege().getId()),
                    cb.or(cb.equal(root.get("sellingReach"), SellingReach.MY_CAMPUS),
                            cb.equal(root.get("sellingReach"), SellingReach.OTHER_COLLEGES), publicReach));
            case NEARBY_COLLEGES -> cb.and(cb.equal(city.get("id"), viewer.getCity().getId()),
                    cb.or(cb.equal(root.get("sellingReach"), SellingReach.OTHER_COLLEGES), publicReach));
            case COMMUNITY_MARKETPLACE -> cb.and(cb.equal(seller.get("accountType"), UserType.COMMUNITY),
                    cb.or(publicReach, otherCollegeReach));
            case ALL_PRODUCTS -> cb.or(publicReach, otherCollegeReach, campusReach);
        };
    }

    public static Specification<Product> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Product> category(UUID categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> productType(ProductType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("productType"), type);
    }

    public static Specification<Product> sellingReach(SellingReach reach) {
        return reach == null ? null : (root, query, cb) -> cb.equal(root.get("sellingReach"), reach);
    }

    public static Specification<Product> college(UUID collegeId) {
        return collegeId == null ? null : (root, query, cb) -> cb.equal(root.get("college").get("id"), collegeId);
    }

    public static Specification<Product> city(UUID cityId) {
        return cityId == null ? null : (root, query, cb) -> cb.equal(root.get("city").get("id"), cityId);
    }

    public static Specification<Product> minimumPrice(BigDecimal price) {
        return price == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Product> maximumPrice(BigDecimal price) {
        return price == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Product> allOf(List<Specification<Product>> specifications) {
        return specifications.stream().filter(java.util.Objects::nonNull)
                .reduce(Specification::and).orElse(null);
    }
}
