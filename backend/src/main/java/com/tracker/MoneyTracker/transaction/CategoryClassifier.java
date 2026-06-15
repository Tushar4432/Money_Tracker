package com.tracker.MoneyTracker.transaction;

import org.springframework.stereotype.Component;

@Component
public class CategoryClassifier {

    public String classify(String details) {
        if (details == null || details.trim().isEmpty()) {
            return "OTHER";
        }

        String lowerDetails = details.trim().toLowerCase();

        // 1. Food
        if (lowerDetails.contains("swiggy") || lowerDetails.contains("zomato")) {
            return "FOOD";
        }

        // 2. Web Shopping
        if (lowerDetails.contains("amazon") || lowerDetails.contains("flipkart")) {
            return "WEB_SHOPPING";
        }

        // 3. Subscription
        if (lowerDetails.contains("netflix") || lowerDetails.contains("spotify")) {
            return "SUBSCRIPTION";
        }

        // 4. Transport
        if (lowerDetails.contains("irctc") || lowerDetails.contains("indianoil") 
                || lowerDetails.contains("petrol") || lowerDetails.contains("fuel")) {
            return "TRANSPORT";
        }

        // 5. Health
        if (lowerDetails.contains("apollo") || lowerDetails.contains("pharmacy") 
                || lowerDetails.contains("hospital") || lowerDetails.contains("clinic")) {
            return "HEALTH";
        }

        // 6. Utilities
        if (lowerDetails.contains("electricity") || lowerDetails.contains("bill")) {
            return "UTILITIES";
        }

        // 7. Income
        if (lowerDetails.contains("salary")) {
            return "INCOME";
        }

        // 8. Transfer
        if (lowerDetails.contains("person") || lowerDetails.contains("transfer") 
                || lowerDetails.contains("neft") || lowerDetails.contains("imps")) {
            return "TRANSFER";
        }

        // 9. Investment
        if (lowerDetails.contains("zerodha")) {
            return "INVESTMENT";
        }

        // 10. Rent
        if (lowerDetails.contains("rent")) {
            return "RENT";
        }

        // 11. Entertainment
        if (lowerDetails.contains("pvr")) {
            return "ENTERTAINMENT";
        }

        // 12. Education
        if (lowerDetails.contains("coursera")) {
            return "EDUCATION";
        }

        return "OTHER";
    }
}

