-- V17: Migrate selling_reach values to CAMPUS_ONLY and OUTSIDE_CAMPUS
UPDATE products SET selling_reach = 'CAMPUS_ONLY' WHERE selling_reach = 'MY_CAMPUS';
UPDATE products SET selling_reach = 'OUTSIDE_CAMPUS' WHERE selling_reach IN ('PUBLIC', 'OTHER_COLLEGES', 'INTRA_CITY', 'ALL_NCR');
