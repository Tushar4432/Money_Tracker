package com.tracker.MoneyTracker.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryClassifier")
class CategoryClassifierTest {

    private CategoryClassifier sut;

    @BeforeEach
    void setUp() {
        sut = new CategoryClassifier();
    }

    @Nested
    @DisplayName("classify")
    class ClassifyTests {

        @Test
        @DisplayName("Should classify UPI food merchant as FOOD")
        void shouldClassifyAsFood_WhenUpiFoodMerchant() {
            // Arrange
            String details = "WDL TFR UPI/DR/123456/SWIGGY/HDFC/swiggy123/Pay";

            // Act
            String category = sut.classify(details);

            // Assert
            assertThat(category).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should classify Zomato transaction as FOOD")
        void shouldClassifyAsFood_WhenZomatoTransaction() {
            String details = "UPI/DR/ZOMATO/ICICI/zomato@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should classify Amazon purchase as WEB_SHOPPING")
        void shouldClassifyAsWebShopping_WhenAmazonPurchase() {
            String details = "UPI/DR/AMAZON/HDFC/amazon@apl/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("WEB_SHOPPING");
        }

        @Test
        @DisplayName("Should classify Flipkart purchase as WEB_SHOPPING")
        void shouldClassifyAsWebShopping_WhenFlipkartPurchase() {
            String details = "UPI/DR/FLIPKART/ICICI/flipkart@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("WEB_SHOPPING");
        }

        @Test
        @DisplayName("Should classify Netflix subscription as SUBSCRIPTION")
        void shouldClassifyAsSubscription_WhenNetflix() {
            String details = "UPI/DR/NETFLIX/HDFC/netflix@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("SUBSCRIPTION");
        }

        @Test
        @DisplayName("Should classify IRCTC transaction as TRANSPORT")
        void shouldClassifyAsTransport_WhenIrctc() {
            String details = "UPI/DR/IRCTC/SBIN/irctc@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should classify fuel/petrol transaction as TRANSPORT")
        void shouldClassifyAsTransport_WhenFuel() {
            String details = "UPI/DR/INDIANOIL/HDFC/fuel@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should classify hospital/pharmacy as HEALTH")
        void shouldClassifyAsHealth_WhenPharmacy() {
            String details = "UPI/DR/APOLLO/HDFC/pharmacy@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should classify electricity bill as UTILITIES")
        void shouldClassifyAsUtilities_WhenElectricityBill() {
            String details = "UPI/DR/ELECTRICITY/HDFC/bill@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("UTILITIES");
        }

        @Test
        @DisplayName("Should classify salary credit as INCOME")
        void shouldClassifyAsIncome_WhenSalaryCredit() {
            String details = "SALARY CREDIT ABC CORP LTD";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("INCOME");
        }

        @Test
        @DisplayName("Should classify UPI transfer as TRANSFER")
        void shouldClassifyAsTransfer_WhenUpiTransfer() {
            String details = "UPI/DR/1234567890/HDFC/person@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should classify Zerodha/stock investment as INVESTMENT")
        void shouldClassifyAsInvestment_WhenZerodha() {
            String details = "UPI/DR/ZERODHA/HDFC/zerodha@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("INVESTMENT");
        }

        @Test
        @DisplayName("Should classify rent payment as RENT")
        void shouldClassifyAsRent_WhenRentPayment() {
            String details = "UPI/DR/RENT/HDFC/landlord@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("RENT");
        }

        @Test
        @DisplayName("Should classify movie/event as ENTERTAINMENT")
        void shouldClassifyAsEntertainment_WhenMovie() {
            String details = "UPI/DR/PVR/HDFC/pvr@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("ENTERTAINMENT");
        }

        @Test
        @DisplayName("Should classify course fee as EDUCATION")
        void shouldClassifyAsEducation_WhenCourseFee() {
            String details = "UPI/DR/COURSERA/HDFC/edu@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("EDUCATION");
        }

        @Test
        @DisplayName("Should classify as OTHER when no keyword matches")
        void shouldClassifyAsOther_WhenNoKeywordMatches() {
            String details = "UPI/DR/UNKNOWN/HDFC/unknown@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should classify as OTHER when details is null")
        void shouldClassifyAsOther_WhenDetailsIsNull() {
            String category = sut.classify(null);

            assertThat(category).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should classify as OTHER when details is blank")
        void shouldClassifyAsOther_WhenDetailsIsBlank() {
            String category = sut.classify("   ");

            assertThat(category).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should classify case-insensitively")
        void shouldClassifyCaseInsensitively() {
            String details = "upi/dr/swiggy/hdfc/SWIGGY@upi/Payment";

            String category = sut.classify(details);

            assertThat(category).isEqualTo("FOOD");
        }
    }
}
