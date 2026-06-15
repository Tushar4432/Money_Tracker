package com.tracker.MoneyTracker.transaction;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CategoryMapper {

    private static final String DEFAULT_CATEGORY = "OTHER";

    /**
     * Map of category name → list of keywords/merchants to match against.
     * Categories are checked in insertion order; first match wins.
     */
    private final LinkedHashMap<String, List<String>> categoryKeywords;

    public CategoryMapper() {
        categoryKeywords = new LinkedHashMap<>();

        categoryKeywords.put("FOOD", List.of(
                "swiggy", "zomato"
        ));

        categoryKeywords.put("WEB_SHOPPING", List.of(
                "amazon", "flipkart"
        ));

        categoryKeywords.put("SUBSCRIPTION", List.of(
                "netflix", "spotify"
        ));

        categoryKeywords.put("TRANSPORT", List.of(
                "irctc", "indianoil", "petrol", "fuel"
        ));

        categoryKeywords.put("HEALTH", List.of(
                "apollo", "pharmacy", "hospital", "clinic"
        ));

        categoryKeywords.put("UTILITIES", List.of(
                "electricity", "bill"
        ));

        categoryKeywords.put("INCOME", List.of(
                "salary"
        ));

        categoryKeywords.put("TRANSFER", List.of(
                "person", "transfer", "neft", "imps"
        ));

        categoryKeywords.put("INVESTMENT", List.of(
                "zerodha"
        ));

        categoryKeywords.put("RENT", List.of(
                "rent"
        ));

        categoryKeywords.put("ENTERTAINMENT", List.of(
                "pvr"
        ));

        categoryKeywords.put("EDUCATION", List.of(
                "coursera"
        ));
    }

    /**
     * Classify a transaction description into a category.
     * Checks each category's keyword list against the description (case-insensitive).
     * First matching category wins. Returns "OTHER" if no match.
     *
     * @param description the transaction description/details text
     * @return the matched category name, or "OTHER"
     */
    public String getCategory(String description) {
        if (description == null || description.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        String lowerDescription = description.trim().toLowerCase();

        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerDescription.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }

        return DEFAULT_CATEGORY;
    }

    /**
     * Returns the full category-to-keywords map.
     *
     * @return an unmodifiable map of category name → list of keywords
     */
    public Map<String, List<String>> getCategories() {
        return Collections.unmodifiableMap(categoryKeywords);
    }

    /**
     * Returns the keyword list for a specific category.
     *
     * @param category the category name
     * @return list of keywords for the category, or empty list if not found
     */
    public List<String> getKeywords(String category) {
        return categoryKeywords.getOrDefault(category, List.of());
    }
}
