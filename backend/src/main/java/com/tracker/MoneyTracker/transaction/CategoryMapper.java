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
                "swiggy", "zomato", "dominos", "pizza hut", "kfc", "mcdonalds",
                "cafe coffee", "starbucks", "biryani", "dhaba", "restaurant",
                "food", "eatery", "snack", "juice", "bakery", "cake", "sweet",
                "grocery", "bigbasket", "zepto", "blinkit", "dmart",
                "reliance fresh", "more supermarket"
        ));

        categoryKeywords.put("WEB_SHOPPING", List.of(
                "amazon", "flipkart", "myntra", "ajio", "meesho", "snapdeal",
                "shopclues", "tatacliq", "nykaa"
        ));

        categoryKeywords.put("SUBSCRIPTION", List.of(
                "netflix", "spotify", "prime video", "hotstar", "disney",
                "youtube premium", "jio cinema", "sony liv", "zee5",
                "apple music", "google play", "adobe", "microsoft 365"
        ));

        categoryKeywords.put("TRANSPORT", List.of(
                "irctc", "uber", "ola", "rapido", "metro", "petrol", "fuel",
                "diesel", "indianoil", "hpcl", "bharatpetroleum", "shell",
                "parking", "toll", "fastag", "nhai"
        ));

        categoryKeywords.put("HEALTH", List.of(
                "apollo", "pharmacy", "hospital", "clinic", "doctor", "medical",
                "medicine", "pharmeasy", "netmeds", "1mg", "health", "dental",
                "eye care", "diagnostic", "lab test"
        ));

        categoryKeywords.put("UTILITIES", List.of(
                "electricity", "water", "gas", "bill", "airtel", "jio", "vi",
                "bsnl", "broadband", "wifi", "fiber", "act", "hathway",
                "tata sky", "dth"
        ));

        categoryKeywords.put("INCOME", List.of(
                "salary", "credit interest", "refund", "dividend",
                "reimbursement", "cashback", "reward"
        ));

        categoryKeywords.put("INVESTMENT", List.of(
                "zerodha", "groww", "upstox", "angel one", "kuvera", "coin",
                "mutual fund", "sip", "stock", "share market", "etmoney", "smallcase"
        ));

        categoryKeywords.put("RENT", List.of(
                "rent", "hostel", "pg", "accommodation", "flat rent", "house rent"
        ));

        categoryKeywords.put("ENTERTAINMENT", List.of(
                "pvr", "inox", "cinepolis", "bookmyshow", "gaming",
                "playstation", "xbox", "steam"
        ));

        categoryKeywords.put("EDUCATION", List.of(
                "coursera", "udemy", "byju", "unacademy", "vedantu",
                "physics wallah", "toppr", "edx", "skillshare",
                "linkedin learning", "tuition", "coaching"
        ));

        categoryKeywords.put("INSURANCE", List.of(
                "lic", "insurance", "policy", "premium",
                "hdfc life", "icici prudential", "sbi life", "max life"
        ));

        categoryKeywords.put("EMI", List.of(
                "emi", "loan", "home loan", "car loan", "personal loan",
                "bajaj finance", "hdfc loan"
        ));

        // TRANSFER must be checked LAST (before OTHER) because keywords like
        // "upi" appear in almost every Indian transaction description.
        categoryKeywords.put("TRANSFER", List.of(
                "neft", "imps", "rtgs", "paytm", "phonepe",
                "google pay", "gpay", "paypal", "bank transfer",
                "person", "transfer"
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
