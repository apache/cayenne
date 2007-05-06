if exists (SELECT * FROM sysobjects WHERE name = 'cayenne_tst_select_proc') 
BEGIN 
	DROP PROCEDURE cayenne_tst_select_proc 
END