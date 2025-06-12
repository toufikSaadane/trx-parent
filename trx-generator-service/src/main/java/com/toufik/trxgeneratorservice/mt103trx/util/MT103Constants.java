package com.toufik.trxgeneratorservice.mt103trx.util;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class MT103Constants {
    private MT103Constants() {} // Prevent instantiation

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    public static final String HEX_CHARS = "ABCDEF0123456789";

    public static final String[] ADDRESS_TEMPLATES = {
            "123 Main Street", "456 Business Ave", "789 Commercial Blvd",
            "321 Financial District", "654 Banking Center", "987 Trade Plaza"
    };

    public static final Map<String, String> COUNTRY_CITIES = Map.ofEntries(
            Map.entry("US", "New York, United States"),
            Map.entry("GB", "London, United Kingdom"),
            Map.entry("DE", "Frankfurt, Germany"),
            Map.entry("FR", "Paris, France"),
            Map.entry("CA", "Toronto, Canada"),
            Map.entry("AU", "Sydney, Australia"),
            Map.entry("JP", "Tokyo, Japan"),
            Map.entry("CH", "Zurich, Switzerland"),
            Map.entry("IT", "Milan, Italy"),
            Map.entry("NL", "Amsterdam, Netherlands"),
            Map.entry("ES", "Madrid, Spain"),
            Map.entry("SG", "Singapore, Singapore"),
            Map.entry("HK", "Hong Kong, Hong Kong"),
            Map.entry("AE", "Dubai, United Arab Emirates"),
            Map.entry("BR", "São Paulo, Brazil"),
            Map.entry("MX", "Mexico City, Mexico"),
            Map.entry("IN", "Mumbai, India"),
            Map.entry("CN", "Shanghai, China"),
            Map.entry("ZA", "Johannesburg, South Africa"),
            Map.entry("KR", "Seoul, South Korea"),
            Map.entry("RU", "Moscow, Russia"),
            Map.entry("TR", "Istanbul, Turkey")
    );

    public static final Map<String, String> INTERMEDIARY_BANKS = Map.ofEntries(
            Map.entry("US", "CHASUS33XXX"),
            Map.entry("GB", "BARCGB22XXX"),
            Map.entry("CA", "TDOMCATTTOR"),
            Map.entry("DE", "DEUTDEFFXXX"),
            Map.entry("FR", "BNPAFRPPXXX"),
            Map.entry("JP", "BOTKJPJTXXX"),
            Map.entry("AU", "ANZBAU3MXXX"),
            Map.entry("CH", "UBSWCHZHXXX"),
            Map.entry("IT", "UNCRITMM XXX"),
            Map.entry("NL", "INGBNL2AXXX"),
            Map.entry("ES", "CAIXESBBXXX")
    );

    public static final Map<String, String> ADDITIONAL_COUNTRIES = Map.ofEntries(
            Map.entry("PT", "Portugal"), Map.entry("BE", "Belgium"), Map.entry("LU", "Luxembourg"),
            Map.entry("AT", "Austria"), Map.entry("FI", "Finland"), Map.entry("NO", "Norway"),
            Map.entry("SE", "Sweden"), Map.entry("DK", "Denmark"), Map.entry("IE", "Ireland"),
            Map.entry("LT", "Lithuania"), Map.entry("LV", "Latvia"), Map.entry("EE", "Estonia"),
            Map.entry("PL", "Poland"), Map.entry("CZ", "Czech Republic"), Map.entry("HU", "Hungary"),
            Map.entry("SK", "Slovakia"), Map.entry("SI", "Slovenia"), Map.entry("HR", "Croatia"),
            Map.entry("BG", "Bulgaria"), Map.entry("RO", "Romania"), Map.entry("GR", "Greece"),
            Map.entry("CY", "Cyprus"), Map.entry("MT", "Malta"), Map.entry("NZ", "New Zealand"),
            Map.entry("KE", "Kenya"), Map.entry("NG", "Nigeria")
    );

}