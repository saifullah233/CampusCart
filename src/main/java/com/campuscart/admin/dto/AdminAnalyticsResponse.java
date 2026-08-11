package com.campuscart.admin.dto;

import java.time.Instant;

public record AdminAnalyticsResponse(
        long totalUsers,
        long activeUsers,
        long totalProducts,
        long activeProducts,
        long soldProducts,
        long totalOrders,
        long completedOrders,
        long totalReports,
        long activeReports,
        long recentProducts,
        long recentOrders,
        long recentReviews,
        long recentMessages,
        long marketplaceActivity,
        Instant generatedAt) {
}
