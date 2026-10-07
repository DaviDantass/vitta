-- Keep demo credentials distinct and aligned with each profile's identifier.
UPDATE users
SET password = CASE email
    WHEN 'doctor.demo@example.test' THEN '$2b$12$0XnRuQbgrBOzjACnBX.SkuCpkOGlO0ODLuXDh9Zz9B38FON8O/Ife'
    WHEN 'patient.demo@example.test' THEN '$2b$12$ucRRhVS/44J3apsRRvv31eX92sSvXSf7rgyPS1sWrCS7GloLJQIpK'
    WHEN 'receptionist.demo@example.test' THEN '$2b$12$IOkCBttbOhjnqGKzP2slDeZBa19.e7/3fTZBGy.TsEFNwTqmuyetS'
END
WHERE email IN ('doctor.demo@example.test', 'patient.demo@example.test', 'receptionist.demo@example.test');
