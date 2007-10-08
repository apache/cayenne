if exists (SELECT * FROM sysobjects WHERE name = 'cayenne_tst_upd_proc') 
BEGIN 
	DROP PROCEDURE cayenne_tst_upd_proc 
END