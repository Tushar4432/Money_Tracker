package com.tracker.MoneyTracker.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryMapper")
class CategoryMapperTest {

    private CategoryMapper sut;

    @BeforeEach
    void setUp() {
        sut = new CategoryMapper();
    }

    @Nested
    @DisplayName("getCategory")
    class GetCategoryTests {

        @Test
        @DisplayName("Should return FOOD when description contains swiggy")
        void shouldReturnFood_WhenSwiggy() {
            assertThat(sut.getCategory("UPI/DR/SWIGGY/HDFC/swiggy@upi")).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should return FOOD when description contains zomato")
        void shouldReturnFood_WhenZomato() {
            assertThat(sut.getCategory("UPI/DR/ZOMATO/ICICI/zomato@upi")).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should return WEB_SHOPPING when description contains amazon")
        void shouldReturnWebShopping_WhenAmazon() {
            assertThat(sut.getCategory("UPI/DR/AMAZON/HDFC/amazon@apl")).isEqualTo("WEB_SHOPPING");
        }

        @Test
        @DisplayName("Should return WEB_SHOPPING when description contains flipkart")
        void shouldReturnWebShopping_WhenFlipkart() {
            assertThat(sut.getCategory("UPI/DR/FLIPKART/ICICI/flipkart@upi")).isEqualTo("WEB_SHOPPING");
        }

        @Test
        @DisplayName("Should return SUBSCRIPTION when description contains netflix")
        void shouldReturnSubscription_WhenNetflix() {
            assertThat(sut.getCategory("UPI/DR/NETFLIX/HDFC/netflix@upi")).isEqualTo("SUBSCRIPTION");
        }

        @Test
        @DisplayName("Should return SUBSCRIPTION when description contains spotify")
        void shouldReturnSubscription_WhenSpotify() {
            assertThat(sut.getCategory("UPI/DR/SPOTIFY/HDFC/spotify@upi")).isEqualTo("SUBSCRIPTION");
        }

        @Test
        @DisplayName("Should return TRANSPORT when description contains irctc")
        void shouldReturnTransport_WhenIrctc() {
            assertThat(sut.getCategory("UPI/DR/IRCTC/SBIN/irctc@upi")).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should return TRANSPORT when description contains petrol")
        void shouldReturnTransport_WhenPetrol() {
            assertThat(sut.getCategory("UPI/DR/PETROL/HDFC/fuel@upi")).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should return TRANSPORT when description contains fuel")
        void shouldReturnTransport_WhenFuel() {
            assertThat(sut.getCategory("UPI/DR/INDIANOIL/HDFC/fuel@upi")).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should return HEALTH when description contains apollo")
        void shouldReturnHealth_WhenApollo() {
            assertThat(sut.getCategory("UPI/DR/APOLLO/HDFC/pharmacy@upi")).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should return HEALTH when description contains pharmacy")
        void shouldReturnHealth_WhenPharmacy() {
            assertThat(sut.getCategory("UPI/DR/PHARMACY/HDFC/pharma@upi")).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should return HEALTH when description contains hospital")
        void shouldReturnHealth_WhenHospital() {
            assertThat(sut.getCategory("UPI/DR/HOSPITAL/HDFC/health@upi")).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should return HEALTH when description contains clinic")
        void shouldReturnHealth_WhenClinic() {
            assertThat(sut.getCategory("UPI/DR/CLINIC/HDFC/clinic@upi")).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should return UTILITIES when description contains electricity")
        void shouldReturnUtilities_WhenElectricity() {
            assertThat(sut.getCategory("UPI/DR/ELECTRICITY/HDFC/bill@upi")).isEqualTo("UTILITIES");
        }

        @Test
        @DisplayName("Should return UTILITIES when description contains bill")
        void shouldReturnUtilities_WhenBill() {
            assertThat(sut.getCategory("UPI/DR/BILL/HDFC/payment@upi")).isEqualTo("UTILITIES");
        }

        @Test
        @DisplayName("Should return INCOME when description contains salary")
        void shouldReturnIncome_WhenSalary() {
            assertThat(sut.getCategory("SALARY CREDIT ABC CORP LTD")).isEqualTo("INCOME");
        }

        @Test
        @DisplayName("Should return TRANSFER when description contains person")
        void shouldReturnTransfer_WhenPerson() {
            assertThat(sut.getCategory("UPI/DR/1234567890/HDFC/person@upi")).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should return TRANSFER when description contains transfer")
        void shouldReturnTransfer_WhenTransfer() {
            assertThat(sut.getCategory("UPI/DR/TRANSFER/HDFC/transfer@upi")).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should return TRANSFER when description contains neft")
        void shouldReturnTransfer_WhenNeft() {
            assertThat(sut.getCategory("NEFT CR/ABC CORP/SBIN")).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should return TRANSFER when description contains imps")
        void shouldReturnTransfer_WhenImps() {
            assertThat(sut.getCategory("IMPS/DR/123456/HDFC")).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should return INVESTMENT when description contains zerodha")
        void shouldReturnInvestment_WhenZerodha() {
            assertThat(sut.getCategory("UPI/DR/ZERODHA/HDFC/zerodha@upi")).isEqualTo("INVESTMENT");
        }

        @Test
        @DisplayName("Should return RENT when description contains rent")
        void shouldReturnRent_WhenRent() {
            assertThat(sut.getCategory("UPI/DR/RENT/HDFC/landlord@upi")).isEqualTo("RENT");
        }

        @Test
        @DisplayName("Should return ENTERTAINMENT when description contains pvr")
        void shouldReturnEntertainment_WhenPvr() {
            assertThat(sut.getCategory("UPI/DR/PVR/HDFC/pvr@upi")).isEqualTo("ENTERTAINMENT");
        }

        @Test
        @DisplayName("Should return EDUCATION when description contains coursera")
        void shouldReturnEducation_WhenCoursera() {
            assertThat(sut.getCategory("UPI/DR/COURSERA/HDFC/edu@upi")).isEqualTo("EDUCATION");
        }

        @Test
        @DisplayName("Should return OTHER when no keyword matches")
        void shouldReturnOther_WhenNoMatch() {
            assertThat(sut.getCategory("UPI/DR/UNKNOWN/HDFC/unknown@upi")).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should return OTHER when description is null")
        void shouldReturnOther_WhenNull() {
            assertThat(sut.getCategory(null)).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should return OTHER when description is blank")
        void shouldReturnOther_WhenBlank() {
            assertThat(sut.getCategory("   ")).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("Should match case-insensitively")
        void shouldMatchCaseInsensitively() {
            assertThat(sut.getCategory("upi/dr/SWIGGY/hdfc/SWIGGY@upi")).isEqualTo("FOOD");
        }
    }

    @Nested
    @DisplayName("expanded Indian merchant keywords")
    class ExpandedMerchantTests {

        @Test
        @DisplayName("Should classify common Indian food merchants correctly")
        void shouldClassifyIndianFoodMerchants() {
            assertThat(sut.getCategory("UPI/DR/DOMINOS/HDFC/dominos@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/KFC/ICICI/kfc@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/STARBUCKS/HDFC/sbux@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/BIGBASKET/HDFC/bb@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/ZEPTO/HDFC/zepto@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/BLINKIT/HDFC/blinkit@upi")).isEqualTo("FOOD");
            assertThat(sut.getCategory("UPI/DR/DMART/HDFC/dmart@upi")).isEqualTo("FOOD");
        }

        @Test
        @DisplayName("Should classify Indian shopping merchants correctly")
        void shouldClassifyIndianShoppingMerchants() {
            assertThat(sut.getCategory("UPI/DR/MYNTRA/HDFC/myntra@upi")).isEqualTo("WEB_SHOPPING");
            assertThat(sut.getCategory("UPI/DR/AJIO/HDFC/ajio@upi")).isEqualTo("WEB_SHOPPING");
            assertThat(sut.getCategory("UPI/DR/NYKAA/HDFC/nykaa@upi")).isEqualTo("WEB_SHOPPING");
            assertThat(sut.getCategory("UPI/DR/TATACLIQ/HDFC/tatacliq@upi")).isEqualTo("WEB_SHOPPING");
        }

        @Test
        @DisplayName("Should classify Indian subscription services correctly")
        void shouldClassifyIndianSubscriptions() {
            assertThat(sut.getCategory("UPI/DR/PRIME VIDEO/HDFC/prime@upi")).isEqualTo("SUBSCRIPTION");
            assertThat(sut.getCategory("UPI/DR/HOTSTAR/HDFC/hotstar@upi")).isEqualTo("SUBSCRIPTION");
            assertThat(sut.getCategory("UPI/DR/JIO CINEMA/HDFC/jiocinema@upi")).isEqualTo("SUBSCRIPTION");
            assertThat(sut.getCategory("UPI/DR/SONY LIV/HDFC/sonyliv@upi")).isEqualTo("SUBSCRIPTION");
        }

        @Test
        @DisplayName("Should classify Indian transport merchants correctly")
        void shouldClassifyIndianTransportMerchants() {
            assertThat(sut.getCategory("UPI/DR/UBER/HDFC/uber@upi")).isEqualTo("TRANSPORT");
            assertThat(sut.getCategory("UPI/DR/OLA/HDFC/ola@upi")).isEqualTo("TRANSPORT");
            assertThat(sut.getCategory("UPI/DR/METRO/HDFC/metro@upi")).isEqualTo("TRANSPORT");
            assertThat(sut.getCategory("UPI/DR/HPCL/HDFC/fuel@upi")).isEqualTo("TRANSPORT");
            assertThat(sut.getCategory("UPI/DR/FASTAG/HDFC/fastag@upi")).isEqualTo("TRANSPORT");
        }

        @Test
        @DisplayName("Should classify Indian health merchants correctly")
        void shouldClassifyIndianHealthMerchants() {
            assertThat(sut.getCategory("UPI/DR/PHARMEASY/HDFC/pharmeasy@upi")).isEqualTo("HEALTH");
            assertThat(sut.getCategory("UPI/DR/NETMEDS/HDFC/netmeds@upi")).isEqualTo("HEALTH");
            assertThat(sut.getCategory("UPI/DR/1MG/HDFC/1mg@upi")).isEqualTo("HEALTH");
        }

        @Test
        @DisplayName("Should classify Indian utility merchants correctly")
        void shouldClassifyIndianUtilityMerchants() {
            assertThat(sut.getCategory("UPI/DR/AIRTEL/HDFC/bill@upi")).isEqualTo("UTILITIES");
            assertThat(sut.getCategory("UPI/DR/JIO/HDFC/recharge@upi")).isEqualTo("UTILITIES");
            assertThat(sut.getCategory("UPI/DR/BROADBAND/HDFC/fiber@upi")).isEqualTo("UTILITIES");
        }

        @Test
        @DisplayName("Should classify Indian investment platforms correctly")
        void shouldClassifyIndianInvestmentPlatforms() {
            assertThat(sut.getCategory("UPI/DR/GROWW/HDFC/groww@upi")).isEqualTo("INVESTMENT");
            assertThat(sut.getCategory("UPI/DR/UPSTOX/HDFC/upstox@upi")).isEqualTo("INVESTMENT");
            assertThat(sut.getCategory("UPI/DR/ANGEL ONE/HDFC/angel@upi")).isEqualTo("INVESTMENT");
            assertThat(sut.getCategory("UPI/DR/KUVERA/HDFC/kuvera@upi")).isEqualTo("INVESTMENT");
        }

        @Test
        @DisplayName("Should classify Indian entertainment merchants correctly")
        void shouldClassifyIndianEntertainmentMerchants() {
            assertThat(sut.getCategory("UPI/DR/INOX/HDFC/inox@upi")).isEqualTo("ENTERTAINMENT");
            assertThat(sut.getCategory("UPI/DR/CINEPOLIS/HDFC/cinepolis@upi")).isEqualTo("ENTERTAINMENT");
            assertThat(sut.getCategory("UPI/DR/BOOKMYSHOW/HDFC/bms@upi")).isEqualTo("ENTERTAINMENT");
        }

        @Test
        @DisplayName("Should classify Indian education platforms correctly")
        void shouldClassifyIndianEducationPlatforms() {
            assertThat(sut.getCategory("UPI/DR/UDMY/HDFC/udemy@upi")).isEqualTo("EDUCATION");
            assertThat(sut.getCategory("UPI/DR/BYJU/HDFC/byjus@upi")).isEqualTo("EDUCATION");
            assertThat(sut.getCategory("UPI/DR/UNACADEMY/HDFC/unacademy@upi")).isEqualTo("EDUCATION");
            assertThat(sut.getCategory("UPI/DR/PHYSICS WALLAH/HDFC/pw@upi")).isEqualTo("EDUCATION");
        }

        @Test
        @DisplayName("Should classify insurance and EMI correctly")
        void shouldClassifyInsuranceAndEmi() {
            assertThat(sut.getCategory("UPI/DR/LIC/HDFC/premium@upi")).isEqualTo("INSURANCE");
            assertThat(sut.getCategory("UPI/DR/INSURANCE/HDFC/policy@upi")).isEqualTo("INSURANCE");
            assertThat(sut.getCategory("UPI/DR/EMI/HDFC/loan@upi")).isEqualTo("EMI");
            assertThat(sut.getCategory("UPI/DR/HOME LOAN/HDFC/emi@upi")).isEqualTo("EMI");
        }

        @Test
        @DisplayName("Should classify income and transfer correctly")
        void shouldClassifyIncomeAndTransfer() {
            assertThat(sut.getCategory("SALARY CREDIT XYZ CORP")).isEqualTo("INCOME");
            assertThat(sut.getCategory("CREDIT INTEREST RECEIVED")).isEqualTo("INCOME");
            assertThat(sut.getCategory("UPI/DR/PAYTM/HDFC/paytm@upi")).isEqualTo("TRANSFER");
            assertThat(sut.getCategory("UPI/DR/PHONEPE/HDFC/phonepe@upi")).isEqualTo("TRANSFER");
            assertThat(sut.getCategory("UPI/DR/GOOGLE PAY/HDFC/gpay@upi")).isEqualTo("TRANSFER");
        }

        @Test
        @DisplayName("Should return OTHER for unknown merchants")
        void shouldReturnOther_ForUnknownMerchants() {
            assertThat(sut.getCategory("UPI/DR/RANDOMXYZ/HDFC/unknown@upi")).isEqualTo("OTHER");
            assertThat(sut.getCategory("SOME RANDOM PAYMENT")).isEqualTo("OTHER");
        }
    }

    @Nested
    @DisplayName("getCategories")
    class GetCategoriesTests {

        @Test
        @DisplayName("Should return all defined categories")
        void shouldReturnAllCategories() {
            Map<String, List<String>> categories = sut.getCategories();

            assertThat(categories).isNotEmpty();
            assertThat(categories).containsKeys(
                    "FOOD", "WEB_SHOPPING", "SUBSCRIPTION", "TRANSPORT",
                    "HEALTH", "UTILITIES", "INCOME", "TRANSFER",
                    "INVESTMENT", "RENT", "ENTERTAINMENT", "EDUCATION"
            );
        }
    }

    @Nested
    @DisplayName("getKeywords")
    class GetKeywordsTests {

        @Test
        @DisplayName("Should return keywords for a given category")
        void shouldReturnKeywordsForCategory() {
            List<String> foodKeywords = sut.getKeywords("FOOD");

            assertThat(foodKeywords).isNotEmpty();
            assertThat(foodKeywords).contains("swiggy", "zomato");
        }

        @Test
        @DisplayName("Should return empty list for unknown category")
        void shouldReturnEmptyListForUnknownCategory() {
            List<String> keywords = sut.getKeywords("NONEXISTENT");
            assertThat(keywords).isEmpty();
        }
    }
}
