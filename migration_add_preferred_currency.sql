-- Migration: Add preferred_currency column to user table
-- This column stores the user's auto-detected currency based on IP geolocation.
-- Supported values: EUR, USD, TND, GBP, MAD (see GeoLocationService.java)

ALTER TABLE `user`
ADD COLUMN `preferred_currency` VARCHAR(3) DEFAULT 'EUR'
AFTER `updated_at`;
