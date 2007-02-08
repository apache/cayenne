if exists (SELECT * FROM sysobjects WHERE name = 'cayenne_tst_upd_proc2') 
BEGIN 
	DROP PROCEDURE cayenne_tst_upd_proc2
END